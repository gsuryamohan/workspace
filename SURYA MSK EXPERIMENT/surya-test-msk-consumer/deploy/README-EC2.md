# surya-msk-consumer EC2 deploy

## Binary
- **JAR:** `target/surya-test-msk-consumer-1.0.0-SNAPSHOT-all.jar` (Maven Shade, main: `SuryaTestConsumer1`)
- **On instance:** `~/msk-consumer/surya-test-msk-consumer-1.0.0-SNAPSHOT-all.jar`

## Instance
| Name | Instance ID | Public IP | IAM profile |
|------|-------------|-----------|-------------|
| surya-msk-consumer | i-0c93f9bdaffae1e76 | 54.86.105.219 | surya-msk-consumer-ec2-profile |

## IAM
Role **`surya-msk-consumer-ec2-role`**: `kafka-cluster:Connect`, `ReadData`, `WriteData`, topic/group ARNs for **demo-cluster-1**.

## Run (foreground)
```bash
export AWS_REGION=us-east-1
export KAFKA_BOOTSTRAP_SERVERS='<BootstrapBrokerStringSaslIam>'
export KAFKA_TOPIC=surya-test-topic
export KAFKA_GROUP_ID=surya-demo-cluster-1-Surya-Test-Consumer-1
java -jar ~/msk-consumer/surya-test-msk-consumer-1.0.0-SNAPSHOT-all.jar
```

## Run (background)
```bash
nohup env AWS_REGION=us-east-1 KAFKA_BOOTSTRAP_SERVERS='...' KAFKA_TOPIC=surya-test-topic \
  java -jar ~/msk-consumer/surya-test-msk-consumer-1.0.0-SNAPSHOT-all.jar >> ~/msk-consumer.log 2>&1 &
```

## Rebuild & copy
```bash
mvn -q package
scp -i KEY.pem target/surya-test-msk-consumer-1.0.0-SNAPSHOT-all.jar ec2-user@54.86.105.219:~/msk-consumer/
```
