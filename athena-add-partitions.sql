ALTER TABLE vpc_flow_logs_db.`surya-vpcflowlogs-hourly` ADD PARTITION (year='2026', month='03', day='07', hour='12')
LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/12/';

ALTER TABLE vpc_flow_logs_db.`surya-vpcflowlogs-hourly` ADD PARTITION (year='2026', month='03', day='07', hour='13')
LOCATION 's3://surya-server-vpc-flow-logs/SimpleMathServiceHours/AWSLogs/359642223676/vpcflowlogs/us-west-2/2026/03/07/13/';
