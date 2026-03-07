# VPC Flow Logs Analysis User Guide

This guide walks you through analyzing VPC Flow Logs stored in Amazon S3 to understand traffic patterns across your VPCs.

**Guide structure:** **Page 1** (Sections 1–8) covers setup, table creation, example queries, and optional steps. **Page 2** covers troubleshooting when the Athena table returns no rows—root cause (including partitioning) and the fix. **Page 3** covers VPC Flow Log 2 (per-hour): Athena table setup, partition key definition, and querying.

---

## Prerequisites

- VPC Flow Logs already publishing to an S3 bucket (e.g. `surya-server-vpc-flow-logs`)
- AWS account access with permissions for S3, Athena, and (optionally) QuickSight
- Flow logs in the same AWS region you use for Athena (e.g. `us-west-2`)

---

## 1. Understand the S3 Layout

Flow logs in S3 follow a path pattern such as:

```
s3://surya-server-vpc-flow-logs/AWSLogs/<account-id>/vpcflowlogs/<region>/<year>/<month>/<day>/
```

**Steps:**

1. Open the **Amazon S3** console and select the bucket `surya-server-vpc-flow-logs`.
2. Navigate the prefixes to confirm objects exist and note the exact path (e.g. `AWSLogs/359642223676/vpcflowlogs/us-west-2/`).
3. Ensure your IAM user or role has at least **s3:GetObject** (and **s3:ListBucket** if needed) on this bucket.

---

## 2. Choose an Analysis Method

| Method | Best for |
|--------|----------|
| **Amazon Athena** | Ad-hoc SQL queries, one-off analysis, reusable reports |
| **Amazon QuickSight** | Dashboards and visualizations on top of Athena or S3 |
| **Download + script** | Custom tools; only practical for small volumes |

This guide uses **Amazon Athena** as the primary method.

---

## 3. Set Up Athena

1. In the AWS Console, open **Amazon Athena** in the **same region** as your flow logs (e.g. **us-west-2**).
2. Go to **Settings** (or the workgroup settings).
3. Set **Query result location** to an S3 path, for example:  
   `s3://surya-server-vpc-flow-logs/athena-results/`
4. Ensure the IAM role used by Athena has:
   - **s3:GetObject** and **s3:ListBucket** on the flow log bucket/prefix
   - **s3:PutObject** and **s3:GetBucketLocation** on the result location path

---

## 4. Create the Flow Logs Table in Athena

### 4.1 Create a database (if needed)

In the Athena Query Editor, run:

```sql
CREATE DATABASE IF NOT EXISTS vpc_flow_logs_db;
```

### 4.2 Create the external table

Replace the bucket name and prefix below with your actual path. If your logs use a different format, adjust the field list to match the [VPC Flow Logs format](https://docs.aws.amazon.com/vpc/latest/userguide/flow-log-records.html).

```sql
CREATE EXTERNAL TABLE vpc_flow_logs_db.flow_logs (
  version int,
  account_id string,
  interface_id string,
  srcaddr string,
  dstaddr string,
  srcport int,
  dstport int,
  protocol int,
  packets bigint,
  bytes bigint,
  start_time bigint,
  end_time bigint,
  action string,
  log_status string
)
PARTITIONED BY (region string, year string, month string, day string)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ' '
LOCATION 's3://surya-server-vpc-flow-logs/AWSLogs/359642223676/vpcflowlogs/us-west-2/'
TBLPROPERTIES ("skip.header.line.count"="1");
```

**Notes:**

- If your S3 layout uses **partition folders** (e.g. `region=us-west-2/year=2026/month=03/day=07/`), set `LOCATION` to the parent prefix and run:

  ```sql
  MSCK REPAIR TABLE vpc_flow_logs_db.flow_logs;
  ```

  so Athena discovers partitions.

- If there is no header line in the log files, remove the `TBLPROPERTIES` line or set `skip.header.line.count` to `0`.

### 4.3 Verify the table

```sql
SELECT * FROM vpc_flow_logs_db.flow_logs LIMIT 10;
```

---

## 5. Example Queries for Traffic Patterns

### 5.1 Traffic by network interface (then map to VPC)

```sql
SELECT interface_id, srcaddr, dstaddr, SUM(packets) AS total_packets, SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db.flow_logs
WHERE log_status = 'OK'
GROUP BY interface_id, srcaddr, dstaddr
ORDER BY total_bytes DESC
LIMIT 100;
```

Use the **EC2 → Network Interfaces** console to map `interface_id` to a VPC and subnet.

### 5.2 Accept vs Reject summary

```sql
SELECT action, COUNT(*) AS flow_count, SUM(packets) AS packets, SUM(bytes) AS bytes
FROM vpc_flow_logs_db.flow_logs
WHERE log_status = 'OK'
GROUP BY action;
```

### 5.3 Top source IPs by traffic volume

```sql
SELECT srcaddr, SUM(bytes) AS total_bytes, SUM(packets) AS total_packets
FROM vpc_flow_logs_db.flow_logs
WHERE log_status = 'OK'
GROUP BY srcaddr
ORDER BY total_bytes DESC
LIMIT 50;
```

### 5.4 Traffic by destination port (e.g. 8080, 22)

```sql
SELECT dstport, action, COUNT(*) AS flows, SUM(bytes) AS bytes
FROM vpc_flow_logs_db.flow_logs
WHERE log_status = 'OK'
GROUP BY dstport, action
ORDER BY bytes DESC;
```

### 5.5 Traffic over time (if using date partitions)

```sql
SELECT day, SUM(bytes) AS total_bytes, COUNT(*) AS flows
FROM vpc_flow_logs_db.flow_logs
WHERE log_status = 'OK'
GROUP BY day
ORDER BY day;
```

### 5.6 Filter by a specific VPC (using known ENI)

Once you know the network interface ID(s) for a VPC, for example `eni-0abc123`:

```sql
SELECT srcaddr, dstaddr, dstport, action, SUM(bytes) AS bytes, SUM(packets) AS packets
FROM vpc_flow_logs_db.flow_logs
WHERE log_status = 'OK' AND interface_id = 'eni-0abc123'
GROUP BY srcaddr, dstaddr, dstport, action
ORDER BY bytes DESC;
```

---

## 6. Optional: Map ENIs to VPCs

- In **EC2 → Network Interfaces**, note the **VPC ID** and **Subnet ID** for each **Network interface ID**.
- You can maintain a small mapping table (e.g. in Athena or a spreadsheet) and join it to `flow_logs` so you can group and filter by VPC or subnet in your queries.

---

## 7. Optional: Visualize with QuickSight

1. In **Amazon QuickSight**, create a new **Dataset**.
2. Choose **Athena** as the source and select the database and table (e.g. `vpc_flow_logs_db.flow_logs`).
3. Create analyses and dashboards (e.g. bytes over time, top source/destination, accept vs reject by interface or VPC).

---

## 8. Quick Reference Checklist

| Step | Action |
|------|--------|
| 1 | Confirm S3 path and permissions for `surya-server-vpc-flow-logs`. |
| 2 | Set Athena query result location and IAM permissions. |
| 3 | Create database and external table; run `MSCK REPAIR TABLE` if partitioned. |
| 4 | Run sample queries to validate and explore traffic patterns. |
| 5 | Map `interface_id` to VPCs for per-VPC analysis. |
| 6 | Optionally build QuickSight dashboards on the Athena table. |

---

## References

- [VPC Flow Logs](https://docs.aws.amazon.com/vpc/latest/userguide/flow-logs.html) – AWS documentation
- [Flow log record format](https://docs.aws.amazon.com/vpc/latest/userguide/flow-log-records.html) – Field definitions
- [Querying flow logs with Athena](https://docs.aws.amazon.com/athena/latest/ug/vpc-flow-logs.html) – Athena integration

---

# Page 2: Troubleshooting — Athena Table Returning No Rows (Root Cause and Fix)

This section explains why an Athena flow logs table can return **no rows** even when S3 has data, what the **root cause** is (including **partitioning**), and the **exact steps** to fix it.

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
- The S3 bucket **did** contain flow log data and objects.

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

Listing the bucket showed:

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
- **Schema:** The table’s columns matched the flow log format; no schema change was required.
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

- **Statement:**  
  `ALTER TABLE vpc_flow_logs_db SET LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathService/AWSLogs/359642223676/vpcflowlogs/us-west-2/';`
- **Effect:** Table’s default base path now includes the `SimpleMathService/` prefix. For partitioned queries, the partition LOCATION still overrides this (see next step).

### Step 3: Point partition `date='2026-03-07'` to the correct path

- **Statement:**  
  `ALTER TABLE vpc_flow_logs_db PARTITION (date='2026-03-07') SET LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathService/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/';`
- **Effect:** For any query with `WHERE date = '2026-03-07'`, Athena now reads **only** from this prefix, where the `.log.gz` files actually exist.

### Step 4: Point partition `date='2026-03-08'` to the same path (as intended)

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

---

# Page 3: VPC Flow Log 2 — Athena Table Setup and Queries (Partition Key)

This section outlines how to create an **Athena table** for **VPC Flow Log 2** (per-hour partition delivery to S3), how to **define and use partition keys**, and how to **query** the data. The focus is on **partition key definition** and the **role partition keys play in queries**.

---

## Flow Log 2 recap

- **Flow log ID:** `fl-0416954785b05e0f8`
- **Destination:** S3 bucket `surya-server-vpc-flow-logs`, prefix **SimpleMathServiceHours**
- **Per-hour partition:** **Yes** — logs are written under `.../year/month/day/hour/` (e.g. `2026/03/07/12/` for 2026-03-07, hour 12 UTC)
- **Path pattern:**  
  `s3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/12/`
- **File format:** `.log.gz` (gzip), space-delimited; same field layout as Flow Log 1

---

## 1. Partition key definition and why it matters

### 1.1 How S3 is organized (per-hour)

Flow Log 2 uses **per-hour partitioning**. Objects land under:

```text
s3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/
└── 2026/
    └── 03/
        └── 07/
            ├── 12/   ← hour (UTC), 0–23
            │   └── *.log.gz
            └── 13/
                └── *.log.gz
```

So each "slice" of data is identified by **year**, **month**, **day**, and **hour**.

### 1.2 Choosing partition keys

- **Partition keys** are the columns you use to align the table with these S3 path segments. For this path layout the natural choice is:
  - **year** (e.g. `'2026'`)
  - **month** (e.g. `'03'`)
  - **day** (e.g. `'07'`)
  - **hour** (e.g. `'12'` for 12:00 UTC)
- All are stored as **string** in Athena (no date/timestamp type for partition columns).
- The **LOCATION** of each partition must point to the exact S3 prefix that contains that year/month/day/hour (e.g. `.../2026/03/07/12/`).

### 1.3 Role of partition keys in queries

- **Partition pruning:** When you filter by partition columns in `WHERE` (e.g. `year='2026' AND month='03' AND day='07' AND hour='12'`), Athena **only reads** the S3 prefix for that partition. It does **not** scan other dates or hours.
- **Cost and performance:** Fewer partitions scanned ⇒ less data read ⇒ lower cost and faster queries. Without partition filters, Athena would scan all data under the table LOCATION.
- **Correctness:** If partition keys or LOCATIONs are wrong, you get no data or wrong data for that partition. So **partition key definition** (names and order) and **partition LOCATION** must match the S3 path layout.

**Summary:** Partition keys define *which S3 prefix* is read for a given query. Defining them to match the path (`year`/`month`/`day`/`hour`) and adding partitions with the correct LOCATION is what makes queries both correct and efficient.

---

## 2. Step 1: Create database (if needed)

In Athena (region **us-west-2**), run:

```sql
CREATE DATABASE IF NOT EXISTS vpc_flow_logs_db;
```

Use the same database as Flow Log 1 if you want all flow log tables in one place.

---

## 3. Step 2: Create the table with partition key definition

Run the following in the Athena Query Editor. Replace the account ID and bucket/prefix if yours differ.

```sql
CREATE EXTERNAL TABLE vpc_flow_logs_db.flow_logs_hourly (
  version int,
  account_id string,
  interface_id string,
  srcaddr string,
  dstaddr string,
  srcport int,
  dstport int,
  protocol int,
  packets bigint,
  bytes bigint,
  start_time bigint,
  end_time bigint,
  action string,
  log_status string
)
PARTITIONED BY (year string, month string, day string, hour string)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ' '
LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/'
TBLPROPERTIES ("skip.header.line.count"="1");
```

**What this does:**

- **PARTITIONED BY (year string, month string, day string, hour string)**  
  Defines the **partition key**: four columns that correspond to the four path levels under the LOCATION (`year/month/day/hour`). Every query that filters on these columns allows Athena to restrict reads to the matching partition(s) only.
- **LOCATION**  
  Base path under which partition paths live (e.g. `.../2026/03/07/12/`). Individual partition LOCATIONs will point to each of these subpaths.
- **skip.header.line.count**  
  Set to `0` if your flow log files have no header line; otherwise leave as `1` if the first line is a header.

Athena will **not** auto-discover partitions for this path style (plain `2026/03/07/12/`), so we add them manually in the next step.

---

## 4. Step 3: Add partitions (map partition key to S3 path)

Each partition is one (year, month, day, hour). You add it by binding those partition key values to the correct S3 prefix.

**Syntax:**

```sql
ALTER TABLE vpc_flow_logs_db.flow_logs_hourly
ADD PARTITION (year='2026', month='03', day='07', hour='12')
LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/12/';
```

**Role of partition key here:**

- The **partition key** `(year, month, day, hour)` appears in the `ADD PARTITION` clause. Those values are what you use in `WHERE` later.
- **LOCATION** must point to the **exact** S3 prefix where that hour's `.log.gz` files are stored. If LOCATION is wrong, queries for that partition return no or wrong data.

**Add every hour you need.** Examples:

```sql
-- 2026-03-07, hour 12 UTC
ALTER TABLE vpc_flow_logs_db.flow_logs_hourly ADD PARTITION (year='2026', month='03', day='07', hour='12')
LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/12/';

-- 2026-03-07, hour 13 UTC
ALTER TABLE vpc_flow_logs_db.flow_logs_hourly ADD PARTITION (year='2026', month='03', day='07', hour='13')
LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/13/';
```

Repeat for other hours/days as data arrives. You can script this (e.g. loop over year/month/day/hour and run `ALTER TABLE ... ADD PARTITION` for each).

---

## 5. Step 4: Query the data (partition key in WHERE)

Once partitions are added, **always** use the partition columns in `WHERE` when possible so Athena only reads the relevant S3 prefixes.

### 5.1 Single hour (best pruning)

```sql
SELECT *
FROM vpc_flow_logs_db.flow_logs_hourly
WHERE year = '2026' AND month = '03' AND day = '07' AND hour = '12'
LIMIT 10;
```

- **Partition key role:** All four partition columns are in `WHERE`, so Athena reads **only** the partition `year=2026/month=03/day=07/hour=12` (that single S3 prefix). Minimal scan, lowest cost.

### 5.2 Full day (multiple hours)

```sql
SELECT srcaddr, dstaddr, action, SUM(bytes) AS total_bytes, SUM(packets) AS total_packets
FROM vpc_flow_logs_db.flow_logs_hourly
WHERE year = '2026' AND month = '03' AND day = '07'
GROUP BY srcaddr, dstaddr, action
ORDER BY total_bytes DESC
LIMIT 20;
```

- **Partition key role:** Filters on `year`, `month`, `day` (no `hour`). Athena reads **all** partitions for that day (all hours under `2026/03/07/`). Still no scan of other days or months.

### 5.3 Single month

```sql
SELECT day, hour, COUNT(*) AS flow_count, SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db.flow_logs_hourly
WHERE year = '2026' AND month = '03'
GROUP BY day, hour
ORDER BY day, hour;
```

- **Partition key role:** Only `year` and `month` in `WHERE`; Athena scans every partition for March 2026 (all days and hours under `2026/03/`).

### 5.4 What to avoid

```sql
-- No partition filter: scans ALL data under the table LOCATION (slow, expensive)
SELECT * FROM vpc_flow_logs_db.flow_logs_hourly LIMIT 10;
```

- **Partition key role:** With no partition filter, Athena does **not** prune; it considers all partitions (and thus all data under the base path). Use this only for small tests or when you really need a full scan.

---

## 6. Summary: partition key definition and role

| Topic | Detail |
|--------|--------|
| **Partition key definition** | `PARTITIONED BY (year string, month string, day string, hour string)` — matches the S3 path `.../year/month/day/hour/`. |
| **Why these keys** | Flow Log 2 delivers with per-hour partitioning; the path has four levels, so four partition columns give one-to-one mapping and precise pruning. |
| **Role in DDL** | Partition keys define which columns Athena uses to decide which partition (and thus which S3 prefix) to read; each `ADD PARTITION` ties one (year, month, day, hour) to one LOCATION. |
| **Role in queries** | Using partition columns in `WHERE` (e.g. `year='2026' AND month='03' AND day='07' AND hour='12'`) restricts the scan to that partition's LOCATION only → **partition pruning** → less data read, lower cost, faster runs. |
| **Best practice** | Always include as many partition key filters as your analysis allows (e.g. at least year and month, ideally year/month/day or year/month/day/hour). |

---

## 7. Quick reference (Flow Log 2)

- **Table:** `vpc_flow_logs_db.flow_logs_hourly`
- **Partition keys:** `year`, `month`, `day`, `hour` (all string)
- **Base path:** `s3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/`
- **Add one partition:**  
  `ALTER TABLE vpc_flow_logs_db.flow_logs_hourly ADD PARTITION (year='YYYY', month='MM', day='DD', hour='HH') LOCATION 's3://.../YYYY/MM/DD/HH/';`
- **Query with pruning:**  
  Always include `WHERE year = '...' AND month = '...' [AND day = '...'] [AND hour = '...']` when possible.
