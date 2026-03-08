# VPC Flow Log Analysis Schema Details

This guide explains the schema of the Athena table `surya-vpcflowlogs-hourly` (and equivalent VPC flow log tables), describing each column and its purpose for VPC flow logs analysis.

**Table:** `vpc_flow_logs_db.surya-vpcflowlogs-hourly`  
**Data source:** VPC Flow Logs (version 2 default format) stored in S3 with per-hour partitioning.

---

## 1. Data Columns (from the flow log record)

| Column | Type | Purpose for VPC flow log analysis |
|--------|------|-----------------------------------|
| **version** | `int` | The VPC Flow Logs format version (typically `2`). Helps identify record format and compatibility. |
| **account_id** | `string` | The AWS account ID that owns the network interface where the traffic was captured. Useful for multi-account analysis and attribution. |
| **interface_id** | `string` | The ID of the network interface (e.g. `eni-xxxxxxxx`) that recorded the flow. Use this to map traffic to a specific VPC, subnet, or EC2 instance via the EC2 → Network Interfaces console. |
| **srcaddr** | `string` | **Source IP address.** For incoming traffic: the external/peer IP. For outgoing traffic: the private IP of the ENI. Core field for identifying who is sending traffic. |
| **dstaddr** | `string` | **Destination IP address.** For outgoing traffic: the target IP. For incoming traffic: the private IP of the ENI. Core field for identifying who is receiving traffic. |
| **srcport** | `int` | Source port (0–65535). For TCP/UDP, typically the ephemeral port on the client side. Helps identify flows and sessions. |
| **dstport** | `int` | Destination port (0–65535). Often indicates the service (e.g. 22=SSH, 443=HTTPS, 8080=custom app). Essential for traffic-by-service analysis. |
| **protocol** | `int` | IANA protocol number (e.g. 6=TCP, 17=UDP, 1=ICMP). Enables filtering and analysis by transport protocol. |
| **packets** | `bigint` | Number of packets in the flow. Used for volume metrics, anomaly detection, and identifying high-packet flows. |
| **bytes** | `bigint` | Number of bytes in the flow. Primary metric for bandwidth and data transfer analysis. |
| **start_time** | `bigint` | Unix timestamp (seconds) of the first packet in the aggregation window. Used for time-based analysis and ordering. |
| **end_time** | `bigint` | Unix timestamp (seconds) of the last packet in the aggregation window. Helps determine flow duration and activity windows. |
| **action** | `string` | `ACCEPT` or `REJECT`. `ACCEPT` = traffic allowed by security groups/NACLs. `REJECT` = traffic blocked. Critical for security analysis and spotting denied traffic. |
| **log_status** | `string` | `OK` = normal; `NODATA` = no traffic in interval; `SKIPDATA` = some records were skipped. Use to assess log completeness. |

---

## 2. Partition Columns (derived from S3 path, not in the log payload)

| Column | Type | Purpose for VPC flow log analysis |
|--------|------|-----------------------------------|
| **year** | `string` | Year from S3 path (e.g. `2026`). Used in `WHERE` for partition pruning and time-range queries. |
| **month** | `string` | Month from S3 path (e.g. `03`). Used for monthly analysis and partition pruning. |
| **day** | `string` | Day from S3 path (e.g. `07`). Used for daily breakdowns and partition pruning. |
| **hour** | `string` | Hour from S3 path (e.g. `12`). Enables hourly traffic patterns; crucial for partition pruning in per-hour tables. |

---

## 3. Common Analysis Use Cases by Column

| Analysis type | Relevant columns |
|---------------|------------------|
| **Top talkers (by bytes/packets)** | `srcaddr`, `dstaddr`, `bytes`, `packets` |
| **Security (ACCEPT vs REJECT)** | `action`, `srcaddr`, `dstaddr`, `dstport` |
| **Application/service traffic** | `dstport`, `srcaddr`, `dstaddr` (e.g. port 8080) |
| **Traffic volume over time** | `bytes`, `packets`, `year`, `month`, `day`, `hour` |
| **Protocol breakdown** | `protocol`, `bytes`, `packets` |
| **Traffic by ENI / instance** | `interface_id`, `srcaddr`, `dstaddr` |

---

## 4. Important Notes

1. **Partition pruning:** Always include `year`, `month`, and (ideally) `day` and `hour` in `WHERE` clauses so Athena only scans relevant S3 partitions. This reduces cost and improves performance.
2. **`interface_id` mapping:** Use the EC2 console (Network Interfaces) to map `interface_id` to instance ID, subnet, VPC, and private IP.
3. **`action = 'REJECT'`:** Indicates traffic blocked by security groups or NACLs; useful for security and troubleshooting.
4. **`bytes` and `packets`** represent aggregated counts per flow over the capture interval (typically 1–10 minutes), not per-packet.

---

## 5. Example Queries Using the Schema

**Top 10 flows by bytes:**
```sql
SELECT srcaddr, dstaddr, SUM(bytes) AS total_bytes, SUM(packets) AS total_packets
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
GROUP BY srcaddr, dstaddr
ORDER BY total_bytes DESC
LIMIT 10;
```

**Rejected flows (security):**
```sql
SELECT srcaddr, dstaddr, dstport, action, SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND action = 'REJECT'
GROUP BY srcaddr, dstaddr, dstport, action
ORDER BY total_bytes DESC;
```

**Traffic by hour:**
```sql
SELECT hour, COUNT(*) AS flow_count, SUM(bytes) AS total_bytes, SUM(packets) AS total_packets
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
GROUP BY hour
ORDER BY hour;
```

---

## 6. Listing VPC Details for Source and Destination Addresses

The flow log table **does not contain VPC IDs** for source or destination IPs. It has `srcaddr`, `dstaddr`, and `interface_id`. VPC details must be derived from **external metadata** (ENI mappings or manual lookup) and joined to the flow log table.

### Option 1: Manual mapping for known IPs (no extra tables)

When you know which IPs belong to which VPC, use `CASE` expressions:

```sql
SELECT
  srcaddr,
  dstaddr,
  CASE
    WHEN srcaddr = '172.31.22.90' THEN 'Server VPC (surya-server)'
    WHEN srcaddr = '54.188.177.201' THEN 'Client VPC 1 (surya-client-1)'
    WHEN srcaddr = '34.213.127.21' THEN 'Client VPC 2 (surya-client-2)'
    WHEN srcaddr LIKE '172.31.%' THEN 'Private (likely same VPC)'
    ELSE 'External/Unknown'
  END AS src_vpc_info,
  CASE
    WHEN dstaddr = '172.31.22.90' THEN 'Server VPC (surya-server)'
    WHEN dstaddr = '54.188.177.201' THEN 'Client VPC 1 (surya-client-1)'
    WHEN dstaddr = '34.213.127.21' THEN 'Client VPC 2 (surya-client-2)'
    WHEN dstaddr LIKE '172.31.%' THEN 'Private (likely same VPC)'
    ELSE 'External/Unknown'
  END AS dst_vpc_info,
  SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
GROUP BY srcaddr, dstaddr
ORDER BY total_bytes DESC;
```

### Option 2: VPC info from the capturing ENI (requires ENI metadata table)

The flow log is captured by one ENI, which belongs to one VPC. To get that VPC:

1. **Create an ENI metadata table** (populate from EC2 DescribeNetworkInterfaces):

```sql
CREATE EXTERNAL TABLE vpc_flow_logs_db.eni_metadata (
  interface_id string,
  vpc_id string,
  subnet_id string,
  instance_id string,
  private_ip string,
  description string
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
LOCATION 's3://your-bucket/eni-metadata/';
```

2. **Populate it** with rows like:
```
eni-00015598130ac74a8    vpc-07905e795d7628027    subnet-xxx    i-096aed3497e86e241    172.31.22.90    Primary
```

3. **Join flow logs with ENI metadata** (VPC of the capturing ENI):

```sql
SELECT
  f.srcaddr,
  f.dstaddr,
  e.vpc_id AS capturing_vpc_id,
  e.subnet_id,
  e.instance_id,
  SUM(f.bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly" f
LEFT JOIN vpc_flow_logs_db.eni_metadata e ON f.interface_id = e.interface_id
WHERE f.year = '2026' AND f.month = '03' AND f.day = '07'
GROUP BY f.srcaddr, f.dstaddr, e.vpc_id, e.subnet_id, e.instance_id
ORDER BY total_bytes DESC;
```

### Option 3: Map both srcaddr and dstaddr using ENI private IPs

If your ENI table has `private_ip`, you can map both source and destination to VPC when they match an ENI in your account:

```sql
WITH eni_lookup AS (
  SELECT interface_id, vpc_id, subnet_id, private_ip
  FROM vpc_flow_logs_db.eni_metadata
)
SELECT
  f.srcaddr,
  f.dstaddr,
  src_eni.vpc_id AS src_vpc_id,
  src_eni.subnet_id AS src_subnet_id,
  dst_eni.vpc_id AS dst_vpc_id,
  dst_eni.subnet_id AS dst_subnet_id
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly" f
LEFT JOIN eni_lookup src_eni ON f.srcaddr = src_eni.private_ip
LEFT JOIN eni_lookup dst_eni ON f.dstaddr = dst_eni.private_ip
WHERE f.year = '2026' AND f.month = '03' AND f.day = '07'
LIMIT 100;
```

This only maps IPs that match ENIs in your ENI table (typically private IPs). External IPs (e.g. public IPs of clients) will remain `NULL`.

### Summary

| Requirement | Approach |
|-------------|----------|
| No extra tables; small set of known IPs | Option 1: `CASE` / manual mapping |
| VPC of the ENI that captured the flow | Option 2: ENI metadata table + join on `interface_id` |
| Map both srcaddr and dstaddr to VPC (when they are ENI private IPs in your account) | Option 3: ENI table with `private_ip`, join on srcaddr and dstaddr |

Flow logs do not include VPC IDs for source and destination; you need one of these mappings or lookup tables.

---

## 7. Analyzing Traffic with Amazon QuickSight

This section outlines how to analyze VPC flow logs from `surya-vpcflowlogs-hourly` using Amazon QuickSight dashboards.

### Prerequisites

1. **Athena table:** `vpc_flow_logs_db.surya-vpcflowlogs-hourly` exists, has partitions, and returns data.
2. **S3:** Athena result location (e.g. `s3://surya-server-vpc-flow-logs/athena-results/`) with IAM access for Athena and QuickSight.
3. **IAM:** QuickSight user/role has Athena (`athena:GetDataCatalog`, `athena:GetDatabase`, `athena:GetTableMetadata`, `athena:ListDatabases`, `athena:ListTableMetadata`, `athena:StartQueryExecution`, `athena:GetQueryExecution`, `athena:GetQueryResults`), Glue (`glue:GetDatabase`, `glue:GetTable`, `glue:GetTables`, `glue:GetPartitions`), and S3 read permissions.

### Step 1: Enable QuickSight

1. Go to **Amazon QuickSight** in the **same region** as your Athena table (e.g. `us-west-2`).
2. Sign up or sign in to QuickSight.
3. Choose an edition (Standard or Enterprise).

### Step 2: Create a Dataset from Athena

1. QuickSight → **Datasets** → **New dataset**.
2. Choose **Athena** as the data source.
3. Enter connection name (e.g. `VPC Flow Logs Athena`).
4. Choose **Create data source**.
5. Select:
   - **Database:** `vpc_flow_logs_db`
   - **Table:** `surya-vpcflowlogs-hourly` (or `"surya-vpcflowlogs-hourly"` if needed)
6. Choose **SPICE** (faster visuals, needs refresh) or **Direct query** (always fresh, can be slower).
7. Click **Edit/Preview data** (optional) to verify.
8. Click **Save & publish** and name the dataset (e.g. `VPC Flow Logs Hourly`).

### Step 3: Configure Fields (optional)

1. In dataset editor, review **Fields**.
2. Optionally add calculated fields (e.g. `total_traffic_mb = bytes / 1048576`).
3. Set dimension vs measure types for `srcaddr`, `dstaddr`, `bytes`, `packets`, etc.

### Step 4: Create an Analysis

1. QuickSight → **Analyses** → **New analysis**.
2. Select the dataset `VPC Flow Logs Hourly`.
3. Add visuals and filters.

### Step 5: Suggested Visuals for VPC Flow Logs

| Visual | Purpose | Fields |
|--------|---------|--------|
| **Bar chart** | Top source IPs by bytes | Dimension: `srcaddr` (top 20), Value: `Sum(bytes)` |
| **Bar chart** | Top destination IPs by bytes | Dimension: `dstaddr`, Value: `Sum(bytes)` |
| **Bar chart** | Top srcaddr–dstaddr pairs | Dimensions: `srcaddr`, `dstaddr`, Value: `Sum(bytes)` |
| **Pie chart** | ACCEPT vs REJECT | Dimension: `action`, Value: `Count(*)` or `Sum(bytes)` |
| **Horizontal bar** | Top ports by bytes | Dimension: `dstport`, Value: `Sum(bytes)` |
| **Line chart** | Traffic over time | X-axis: `year`, `month`, `day`, `hour` (or combined date), Value: `Sum(bytes)` |
| **KPI** | Total flows | Value: `Count(*)` |
| **KPI** | Total bytes | Value: `Sum(bytes)` |
| **KPI** | Total packets | Value: `Sum(packets)` |
| **Table** | Detail view | Columns: `srcaddr`, `dstaddr`, `action`, `bytes`, `packets`, etc. |

### Step 6: Add Filters

1. Add **Filters** on partition columns to reduce scan and cost: `year`, `month`, `day`, `hour`.
2. Add filters on `action` (ACCEPT/REJECT), `dstport` (e.g. 8080), or `srcaddr`/`dstaddr` as needed.

### Step 7: Publish Dashboard

1. **Share** → **Publish dashboard**.
2. Name the dashboard (e.g. `VPC Flow Logs Analysis`).
3. Share with users or groups if required.

### Step 8: Refresh SPICE (if using SPICE)

1. QuickSight → **Datasets** → select the dataset.
2. **Schedule refresh** and set interval (e.g. daily or every few hours).
3. Or use **Refresh** manually when needed.

### Checklist

- [ ] Athena table exists and returns data
- [ ] QuickSight and Athena in same region
- [ ] IAM permissions set for Athena, Glue, S3
- [ ] Dataset created from Athena
- [ ] SPICE or Direct query selected
- [ ] Visuals added (bytes, packets, top talkers, ACCEPT vs REJECT, time series)
- [ ] Filters added (year, month, day, action, etc.)
- [ ] Dashboard published and shared (if needed)
- [ ] SPICE refresh scheduled (if using SPICE)
