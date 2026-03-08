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
