# VPC Flow Log 2: Athena Table Setup and Query Guide

This guide outlines how to create an **Athena table** for **VPC Flow Log 2** (per-hour partition delivery to S3), how to **define and use partition keys**, and how to **query** the data. The focus is on **partition key definition** and the **role partition keys play in queries**.

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

So each “slice” of data is identified by **year**, **month**, **day**, and **hour**.

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
- **LOCATION** must point to the **exact** S3 prefix where that hour’s `.log.gz` files are stored. If LOCATION is wrong, queries for that partition return no or wrong data.

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
| **Role in queries** | Using partition columns in `WHERE` (e.g. `year='2026' AND month='03' AND day='07' AND hour='12'`) restricts the scan to that partition’s LOCATION only → **partition pruning** → less data read, lower cost, faster runs. |
| **Best practice** | Always include as many partition key filters as your analysis allows (e.g. at least year and month, ideally year/month/day or year/month/day/hour). |

---

## 7. Quick reference

- **Table:** `vpc_flow_logs_db.flow_logs_hourly`
- **Partition keys:** `year`, `month`, `day`, `hour` (all string)
- **Base path:** `s3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/`
- **Add one partition:**  
  `ALTER TABLE vpc_flow_logs_db.flow_logs_hourly ADD PARTITION (year='YYYY', month='MM', day='DD', hour='HH') LOCATION 's3://.../YYYY/MM/DD/HH/';`
- **Query with pruning:**  
  Always include `WHERE year = '...' AND month = '...' [AND day = '...'] [AND hour = '...']` when possible.
