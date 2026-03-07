CREATE EXTERNAL TABLE vpc_flow_logs_db.`surya-vpcflowlogs-hourly` (
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
