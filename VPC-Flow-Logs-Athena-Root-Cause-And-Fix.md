# VPC Flow Logs in Athena: What Went Wrong and How It Was Fixed

This document explains why the Athena table `vpc_flow_logs_db.vpc_flow_logs_db` initially returned **no rows**, what the **root cause** was (including **partitioning**), and the **exact steps** taken to fix it.

---

## 1. What Was Done Initially (Before the Fix)

### 1.1 Table and partition setup

- **Database:** `vpc_flow_logs_db`
- **Table:** `vpc_flow_logs_db.vpc_flow_logs_db`
- **Partition key:** `date` (type `string`), e.g. `'2026-03-07'`, `'2026-03-08'`
- **Table LOCATION (base path):**  
  `s3://surya-server-vpc-flow-logs/AWSLogs/359642223676/vpcflowlogs/us-west-2/`
- **Partition 1:** `date='2026-03-07'` → LOCATION  
  `s3://surya-server-vpc-flow-logs/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/`
- **Partition 2:** `date='2026-03-08'` → LOCATION  
  `s3://surya-server-vpc-flow-logs/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/`  
  (same path as 2026-03-07, as requested.)

So the table was **partitioned by date**, and each partition had an explicit **partition LOCATION** pointing to a specific S3 prefix.

### 1.2 What we observed

- Queries such as  
  `SELECT * FROM vpc_flow_logs_db.vpc_flow_logs_db WHERE date = '2026-03-07' LIMIT 10`  
  returned **only the header row** and **zero data rows**.
- The same happened for `date = '2026-03-08'` (same underlying path).
- You confirmed that the S3 bucket **did** contain flow log data and objects.

So the problem was **not** “no data in S3” but **where** Athena was reading from.

---

## 2. Root Cause: Wrong S3 Path (No Data at the Path We Pointed To)

### 2.1 How Athena uses paths for partitioned tables

- For a **partitioned** table, when you run a query with a partition filter (e.g. `WHERE date = '2026-03-07'`), Athena **does not** use the table’s base `LOCATION` for that query.
- It uses **only** the **partition’s own LOCATION** for that partition.
- So the path that matters for `WHERE date = '2026-03-07'` is **exactly** the LOCATION we gave when we ran  
  `ADD PARTITION (date='2026-03-07') LOCATION '...'`.

So:

- **Partitioning** was set up correctly (partition key `date`, and partitions were added).
- The **failure** was that the **partition LOCATION** (and the table LOCATION) pointed to an S3 prefix where **no flow log objects existed**.

### 2.2 The actual S3 layout

We listed the bucket and found:

- At the **bucket root**, there was **no** `AWSLogs/` folder.
- Flow log objects lived under a **different prefix**: **`SimpleMathService/`**.

Actual layout:

```text
s3://surya-server-vpc-flow-logs/
├── SimpleMathService/
│   └── AWSLogs/
│       └── 359642223676/
│           └── vpcflowlogs/
│               └── us-west-2/
│                   └── 2026/
│                       └── 03/
│                           └── 07/
│                               ├── 359642223676_vpcflowlogs_us-west-2_fl-..._20260307T1015Z_....log.gz
│                               ├── 359642223676_vpcflowlogs_us-west-2_fl-..._20260307T1020Z_....log.gz
│                               └── ... (many .log.gz files)
├── athena-results/
└── ...
```

So the **correct** path for 2026/03/07 data is:

- **Correct:**  
  `s3://surya-server-vpc-flow-logs/SimpleMathService/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/`

What we had configured:

- **Incorrect (missing prefix):**  
  `s3://surya-server-vpc-flow-logs/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/`

Athena only reads from the paths we give it. At the incorrect path there were **no objects**, so the query returned **no rows**. The **root cause** was this **path mismatch**, not partitioning itself.

### 2.3 Why partitioning made the problem clear

- **Without partitions:** we might have pointed the table at one wrong base path and seen no data everywhere.
- **With partitions:** we pointed **each partition** at a specific path. So when we fixed the path for a partition, **only that partition** started returning data. That made it obvious that:
  - The **partition LOCATION** is what actually drives where Athena reads for that partition.
  - Getting the **exact S3 prefix** right (including the `SimpleMathService/` segment) was critical.

So the **root cause** was: **wrong S3 path (missing `SimpleMathService/`)**; **partitioning** is what made the “which path is used” behavior explicit and fixable per partition.

---

## 3. What Else Could Have Gone Wrong (And Didn’t)

- **File format:** Objects are `.log.gz` (gzip). Athena can read gzip-compressed, space-delimited flow logs from that path; no change was needed.
- **Schema:** The table’s columns (version, account_id, interface_id, srcaddr, dstaddr, srcport, dstport, protocol, packets, bytes, start_time, end_time, action, log_status) matched the flow log format; no schema change was required.
- **Partition key type:** Using `date` as a `string` (e.g. `'2026-03-07'`) is correct for this path layout; no change was needed.

So the **only** change required was fixing the **S3 paths** used by the table and its partitions.

---

## 4. Steps Taken to Fix the Problem

### Step 1: Confirm where the data actually is

- Listed the bucket:  
  `aws s3 ls s3://surya-server-vpc-flow-logs/ --recursive`
- Observed that flow log objects exist only under  
  `SimpleMathService/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/`  
  and that files are `.log.gz`.

### Step 2: Set the table’s default LOCATION to the correct base path

- So that any new or unpartitioned data would be looked for in the right place:
- **Statement:**  
  `ALTER TABLE vpc_flow_logs_db SET LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathService/AWSLogs/359642223676/vpcflowlogs/us-west-2/';`
- **Effect:** Table’s default base path now includes the `SimpleMathService/` prefix. For partitioned queries, the partition LOCATION still overrides this (see next step).

### Step 3: Point partition `date='2026-03-07'` to the correct path

- **Statement:**  
  `ALTER TABLE vpc_flow_logs_db PARTITION (date='2026-03-07') SET LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathService/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/';`
- **Effect:** For any query with `WHERE date = '2026-03-07'`, Athena now reads **only** from this prefix, where the `.log.gz` files actually exist.

### Step 4: Point partition `date='2026-03-08'` to the same path (as intended)

- You had specified using only 2026/03/07 data; partition `date='2026-03-08'` was also added with LOCATION pointing to 2026/03/07.
- **Statement:**  
  `ALTER TABLE vpc_flow_logs_db PARTITION (date='2026-03-08') SET LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathService/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/';`
- **Effect:** Queries with `WHERE date = '2026-03-08'` now also read from the 2026/03/07 data path.

### Step 5: Re-run the query and confirm data

- **Query:**  
  `SELECT * FROM vpc_flow_logs_db.vpc_flow_logs_db WHERE date = '2026-03-07' LIMIT 10;`
- **Result:** Query succeeded and returned multiple data rows (flow log records with srcaddr, dstaddr, action, etc.), confirming that the partition LOCATION fix was correct.

---

## 5. Summary: Root Cause and Partitioning

| Aspect | What was wrong | What fixed it |
|--------|----------------|----------------|
| **Root cause** | Table and partition LOCATIONs used a path **without** the `SimpleMathService/` prefix. No objects existed at that path, so Athena read nothing. | Use the **actual** S3 prefix where objects exist: include `SimpleMathService/` in every LOCATION. |
| **Partitioning** | Partitions were correctly defined (partition key `date`) and added, but their **LOCATION** was wrong. For partitioned queries, Athena uses **only** the partition LOCATION, so wrong path ⇒ 0 rows for that partition. | `ALTER TABLE ... PARTITION (date='...') SET LOCATION '...'` to point each partition at the correct S3 prefix. |
| **Table LOCATION** | Table’s default LOCATION also missed `SimpleMathService/`. | `ALTER TABLE ... SET LOCATION '...'` so the table’s base path is correct for future partitions or non-partitioned reads. |

**Takeaway:** For partitioned Athena tables, the **partition LOCATION** is what controls where data is read for that partition. The path must **exactly** match the S3 prefix where the files (e.g. `.log.gz` flow logs) are stored. In this case, the missing `SimpleMathService/` segment in the path was the root cause; correcting the paths (including for each partition) fixed the problem.
