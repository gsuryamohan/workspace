# VPC Flow Logs Analysis User Guide

This guide walks you through analyzing VPC Flow Logs stored in Amazon S3 to understand traffic patterns across your VPCs.

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
