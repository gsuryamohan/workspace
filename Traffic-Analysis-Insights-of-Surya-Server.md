# Traffic Analysis Insights of Surya Server

This guide presents analysis of VPC Flow Logs for the Surya server (SimpleMath service) using the Athena table `vpc_flow_logs_db.surya-vpcflowlogs-hourly`. Each section is a separate page with a clear heading, the analysis query, and the captured output.

**Data scope:** 2026-03-07, hours 12 and 13 (UTC).  
**Service IP:** 172.31.22.90 (surya-service).  
**Known clients:** 34.213.127.21 (surya-client-2), 54.188.177.201 (surya-client-1).

---

## Page 1: Overview — Total Traffic Summary

**Heading:** Overview — Total Traffic Summary

**Purpose:** High-level counts of flow records, bytes, and packets for the analysis window.

**Query:**

```sql
SELECT
  COUNT(*) AS total_flows,
  SUM(bytes) AS total_bytes,
  SUM(packets) AS total_packets
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07';
```

**Output:**

| total_flows | total_bytes | total_packets |
|-------------|-------------|---------------|
| 1418        | 17,253,434  | 135,083       |

**Insight:** Over the two hours, the VPC saw 1,418 flow records, ~17.3 MB of traffic, and 135,083 packets.

---

## Page 2: Traffic by Action (ACCEPT vs REJECT)

**Heading:** Traffic by Action (ACCEPT vs REJECT)

**Purpose:** Break down traffic by flow outcome to compare allowed vs denied traffic.

**Query:**

```sql
SELECT
  action,
  COUNT(*) AS flow_count,
  SUM(packets) AS total_packets,
  SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
GROUP BY action
ORDER BY total_bytes DESC;
```

**Output:**

| action | flow_count | total_packets | total_bytes |
|--------|------------|---------------|-------------|
| ACCEPT | 544        | 134,110       | 17,204,819  |
| REJECT | 873        | 973           | 48,615      |
| -      | 1          | (null)        | (null)      |

**Insight:** Most traffic is ACCEPT (by flow count, packets, and bytes). REJECT flows are numerous (873) but small in volume (48,615 bytes), consistent with security rules blocking unwanted probes.

---

## Page 3: Top Talkers by Traffic Volume

**Heading:** Top Talkers by Traffic Volume

**Purpose:** Identify the largest source–destination pairs by bytes and packets.

**Query:**

```sql
SELECT
  srcaddr,
  dstaddr,
  SUM(packets) AS total_packets,
  SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
GROUP BY srcaddr, dstaddr
ORDER BY total_bytes DESC
LIMIT 20;
```

**Output:**

| srcaddr        | dstaddr       | total_packets | total_bytes |
|----------------|---------------|---------------|-------------|
| 34.213.127.21  | 172.31.22.90  | 33,478        | 4,917,426   |
| 54.188.177.201 | 172.31.22.90  | 33,454        | 4,862,050   |
| 172.31.22.90   | 34.213.127.21 | 33,456        | 3,748,536   |
| 172.31.22.90   | 54.188.177.201| 33,620        | 3,669,055   |
| 172.31.22.90   | 52.26.174.155 | 23            | 1,748       |
| 52.26.174.155  | 172.31.22.90  | 23            | 1,748       |
| 216.180.246.108| 172.31.22.90  | 36            | 1,584       |
| 54.188.179.186 | 172.31.22.90  | 14            | 1,064       |
| 172.31.22.90   | 54.188.179.186| 14            | 1,064       |
| 216.180.246.222| 172.31.22.90  | 17            | 1,008       |
| 200.32.226.72  | 172.31.22.90  | 1             | 552         |
| 113.201.207.78 | 172.31.22.90  | 1             | 540         |
| 172.31.22.90   | 35.88.92.45   | 7             | 532         |
| 35.88.92.45    | 172.31.22.90  | 7             | 532         |
| 172.31.22.90   | 35.91.110.229 | 7             | 532         |
| 35.91.110.229  | 172.31.22.90  | 7             | 532         |
| 188.165.26.231 | 172.31.22.90  | 1             | 478         |
| 15.204.54.13   | 172.31.22.90  | 1             | 440         |
| 207.244.246.135| 172.31.22.90  | 11            | 440         |
| 45.166.100.18  | 172.31.22.90  | 1             | 438         |

**Insight:** The top four pairs are the two known clients (34.213.127.21 and 54.188.177.201) talking to the server (172.31.22.90) and the server’s responses. All other pairs are much smaller; many are likely scans or one-off connections.

---

## Page 4: Application Traffic (Port 8080 — SimpleMath Service)

**Heading:** Application Traffic (Port 8080 — SimpleMath Service)

**Purpose:** Focus on traffic to or from port 8080 (SimpleMath API).

**Query:**

```sql
SELECT
  srcaddr,
  dstaddr,
  dstport,
  SUM(packets) AS total_packets,
  SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
  AND (dstport = 8080 OR srcport = 8080)
GROUP BY srcaddr, dstaddr, dstport
ORDER BY total_bytes DESC
LIMIT 20;
```

**Output (top rows):**

| srcaddr        | dstaddr       | dstport | total_packets | total_bytes |
|----------------|---------------|---------|---------------|-------------|
| 34.213.127.21  | 172.31.22.90  | 8080    | 33,478        | 4,917,426   |
| 54.188.177.201 | 172.31.22.90  | 8080    | 33,454        | 4,862,050   |
| 172.31.22.90   | 34.213.127.21 | 35566   | 313           | 34,532      |
| 172.31.22.90   | 34.213.127.21 | 35582   | 312           | 34,406      |
| 172.31.22.90   | 34.213.127.21 | 35658   | 307           | 34,156      |
| 172.31.22.90   | 34.213.127.21 | 56038   | 307           | 34,141      |
| 172.31.22.90   | 34.213.127.21 | 50524   | 307           | 34,131      |
| 172.31.22.90   | 34.213.127.21 | 47664   | 307           | 34,125      |
| 172.31.22.90   | 34.213.127.21 | 43876   | 307           | 34,110      |
| 172.31.22.90   | 34.213.127.21 | 43838   | 304           | 34,103      |
| ...            | ...           | ...     | ...           | ...         |

**Insight:** The largest port-8080 traffic is from the two client IPs to 172.31.22.90:8080. Rows with different `dstport` (e.g. 35566, 35582) are return traffic from the server to client ephemeral ports; together they represent the SimpleMath API usage.

---

## Page 5: Rejected Flows (Security Insight)

**Heading:** Rejected Flows (Security Insight)

**Purpose:** List traffic that was denied by the security group or NACL, to spot probes and unwanted access attempts.

**Query:**

```sql
SELECT
  srcaddr,
  dstaddr,
  dstport,
  SUM(packets) AS total_packets,
  SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
  AND action = 'REJECT'
GROUP BY srcaddr, dstaddr, dstport
ORDER BY total_bytes DESC;
```

**Output (sample — top 10 by bytes):**

| srcaddr         | dstaddr      | dstport | total_packets | total_bytes |
|-----------------|--------------|---------|---------------|-------------|
| 216.180.246.222 | 172.31.22.90 | 53      | 17            | 1,008       |
| 216.180.246.108 | 172.31.22.90 | 993     | 16            | 704         |
| 216.180.246.108 | 172.31.22.90 | 45000   | 16            | 704         |
| 200.32.226.72   | 172.31.22.90 | 39914   | 1             | 552         |
| 113.201.207.78  | 172.31.22.90 | 248     | 1             | 540         |
| 188.165.26.231  | 172.31.22.90 | 5060    | 1             | 478         |
| 15.204.54.13    | 172.31.22.90 | 5060    | 1             | 440         |
| 207.244.246.135 | 172.31.22.90 | 443     | 11            | 440         |
| 45.166.100.18   | 172.31.22.90 | 8080    | 1             | 438         |
| 45.79.170.73    | 172.31.22.90 | 8080    | 1             | 44          |

**Insight:** Rejected flows target 172.31.22.90 on various ports (e.g. 53, 443, 993, 5060, 8080, 39914, 45000). These are likely internet scans or connection attempts correctly blocked by security rules. Port 8080 rejections show probes to the application port from non-whitelisted IPs.

---

## Page 6: Hourly Traffic Pattern

**Heading:** Hourly Traffic Pattern

**Purpose:** Compare flow count, packets, and bytes across hours for the same day.

**Query:**

```sql
SELECT
  year,
  month,
  day,
  hour,
  COUNT(*) AS flow_count,
  SUM(packets) AS total_packets,
  SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
GROUP BY year, month, day, hour
ORDER BY hour;
```

**Output:**

| year | month | day | hour | flow_count | total_packets | total_bytes |
|------|-------|-----|------|------------|---------------|-------------|
| 2026 | 03    | 07  | 12   | 653        | 61,388        | 7,841,310   |
| 2026 | 03    | 07  | 13   | 765        | 73,695        | 9,412,124   |

**Insight:** Hour 13 has more flows, packets, and bytes than hour 12, indicating a slight increase in activity in the second hour of the analysis window.

---

## Page 7: Hourly Traffic Analysis between Client and Server VPCs

**Heading:** Hourly Traffic Analysis between Client and Server VPCs

**Purpose:** Analyze traffic by VPC: which client VPC sends how much to the server, how much the server VPC serves to each client VPC, and an hourly breakup of client VPC traffic to the server VPC.

**VPC mapping (from flow log source/destination IPs):**

| Role    | IP address     | VPC / component                                      |
|---------|----------------|------------------------------------------------------|
| Server  | 172.31.22.90   | **Server VPC** (surya-server VPC — where flow logs are captured) |
| Client 1| 54.188.177.201 | **Client VPC 1** (surya-client-1)                    |
| Client 2| 34.213.127.21  | **Client VPC 2** (surya-client-2)                    |

---

### 1. Traffic sent by each client VPC to the server (totals)

**Query:**

```sql
WITH to_server AS (
  SELECT srcaddr AS client_ip,
         SUM(bytes) AS bytes_sent,
         SUM(packets) AS packets_sent
  FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
  WHERE year = '2026' AND month = '03' AND day = '07'
    AND dstaddr = '172.31.22.90'
    AND srcaddr IN ('34.213.127.21', '54.188.177.201')
  GROUP BY srcaddr
),
from_server AS (
  SELECT dstaddr AS client_ip,
         SUM(bytes) AS bytes_received,
         SUM(packets) AS packets_received
  FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
  WHERE year = '2026' AND month = '03' AND day = '07'
    AND srcaddr = '172.31.22.90'
    AND dstaddr IN ('34.213.127.21', '54.188.177.201')
  GROUP BY dstaddr
)
SELECT COALESCE(t.client_ip, f.client_ip) AS client_ip,
       COALESCE(t.bytes_sent, 0) AS bytes_sent_to_server,
       COALESCE(t.packets_sent, 0) AS packets_sent_to_server,
       COALESCE(f.bytes_received, 0) AS bytes_received_from_server,
       COALESCE(f.packets_received, 0) AS packets_received_from_server
FROM to_server t
FULL OUTER JOIN from_server f ON t.client_ip = f.client_ip;
```

**Output:**

| client_ip      | bytes_sent_to_server | packets_sent_to_server | bytes_received_from_server | packets_received_from_server |
|----------------|----------------------|-------------------------|----------------------------|------------------------------|
| 34.213.127.21  | 4,917,426            | 33,478                  | 3,748,536                  | 33,456                       |
| 54.188.177.201 | 4,862,050            | 33,454                  | 3,669,055                  | 33,620                       |

**Insight:** Client VPC 2 (34.213.127.21) sent slightly more to the server (~4.92 MB) and received ~3.75 MB. Client VPC 1 (54.188.177.201) sent ~4.86 MB and received ~3.67 MB. Both client VPCs have similar bidirectional usage.

---

### 2. Traffic served by the Server VPC to client VPCs (totals)

**Derived from the same data:** The Server VPC (172.31.22.90) served the following to the two client VPCs combined:

| Metric              | Total served to client VPCs |
|---------------------|-----------------------------|
| Total bytes         | 7,417,591 (3,748,536 + 3,669,055) |
| Total packets       | 67,076 (33,456 + 33,620)    |

Total traffic received by the server from client VPCs: **9,779,476 bytes** (4,917,426 + 4,862,050) and **66,932 packets**. So the server VPC received slightly more bytes from clients than it sent back (request payloads vs response payloads).

---

### 3. Hourly breakup: Client VPCs → Server VPC (traffic sent to server by hour)

**Query:**

```sql
SELECT hour,
       srcaddr AS client_ip,
       COUNT(*) AS flow_count,
       SUM(packets) AS total_packets,
       SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
  AND dstaddr = '172.31.22.90'
  AND srcaddr IN ('34.213.127.21', '54.188.177.201')
GROUP BY hour, srcaddr
ORDER BY hour, total_bytes DESC;
```

**Output:**

| hour | client_ip      | flow_count | total_packets | total_bytes |
|------|----------------|------------|---------------|-------------|
| 12   | 34.213.127.21  | 50         | 15,212        | 2,234,923   |
| 12   | 54.188.177.201 | 50         | 15,202        | 2,209,837   |
| 13   | 34.213.127.21  | 60         | 18,266        | 2,682,503   |
| 13   | 54.188.177.201 | 61         | 18,252        | 2,652,213   |

**Insight:** In hour 12, both client VPCs sent a similar amount to the server (~2.2 MB each). In hour 13, traffic from both increased (Client VPC 2 ~2.68 MB, Client VPC 1 ~2.65 MB). Hour 13 has more flows and bytes than hour 12 for both.

---

### 4. Hourly breakup: Server VPC → Client VPCs (traffic served to each client by hour)

**Query:**

```sql
SELECT hour,
       dstaddr AS client_ip,
       COUNT(*) AS flow_count,
       SUM(packets) AS total_packets,
       SUM(bytes) AS total_bytes
FROM vpc_flow_logs_db."surya-vpcflowlogs-hourly"
WHERE year = '2026' AND month = '03' AND day = '07'
  AND srcaddr = '172.31.22.90'
  AND dstaddr IN ('34.213.127.21', '54.188.177.201')
GROUP BY hour, dstaddr
ORDER BY hour, total_bytes DESC;
```

**Output:**

| hour | client_ip      | flow_count | total_packets | total_bytes |
|------|----------------|------------|---------------|-------------|
| 12   | 34.213.127.21  | 50         | 15,200        | 1,703,644   |
| 12   | 54.188.177.201 | 50         | 15,288        | 1,667,895   |
| 13   | 34.213.127.21  | 60         | 18,256        | 2,044,892   |
| 13   | 54.188.177.201 | 61         | 18,332        | 2,001,160   |

**Insight:** The server VPC served more traffic in hour 13 than in hour 12 to both client VPCs. Client VPC 2 received slightly more bytes than Client VPC 1 in both hours.

---

### Page 7 summary

| Aspect                         | Detail |
|--------------------------------|--------|
| **Client VPCs sending**        | Client VPC 2: 4.92 MB sent, 3.75 MB received. Client VPC 1: 4.86 MB sent, 3.67 MB received. |
| **Server VPC serving**         | Total bytes served to client VPCs: 7.42 MB. Total received from client VPCs: 9.78 MB. |
| **Hourly breakup (client→server)** | Hour 12: ~4.44 MB total from both clients; Hour 13: ~5.33 MB total from both clients. |
| **Hourly breakup (server→client)** | Hour 12: ~3.37 MB total to both clients; Hour 13: ~4.05 MB total to both clients. |

---

## Summary

| Page | Heading                                                   | Main takeaway                                              |
|------|-----------------------------------------------------------|------------------------------------------------------------|
| 1    | Overview — Total Traffic Summary                           | 1,418 flows, ~17.3 MB, 135K packets in 2 hours.            |
| 2    | Traffic by Action (ACCEPT vs REJECT)                       | Most traffic ACCEPT; REJECT is many flows but low volume.  |
| 3    | Top Talkers by Traffic Volume                             | Known clients and server dominate; rest is minor.            |
| 4    | Application Traffic (Port 8080)                           | SimpleMath (8080) traffic is from the two client IPs.       |
| 5    | Rejected Flows (Security Insight)                         | Rejects are mostly probes to various ports, including 8080.|
| 6    | Hourly Traffic Pattern                                    | Slight increase from hour 12 to hour 13.                     |
| 7    | Hourly Traffic Analysis between Client and Server VPCs    | Client/server VPC totals and per-hour breakup (client→server, server→client). |

All queries use the table `vpc_flow_logs_db."surya-vpcflowlogs-hourly"` with partition filters `year = '2026' AND month = '03' AND day = '07'` (and optionally `hour`) for partition pruning. Re-run with different date/hour filters to analyze other time windows.
