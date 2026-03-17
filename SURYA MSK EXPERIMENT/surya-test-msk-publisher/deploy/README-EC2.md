# Deploy on `surya-msk-publisher` EC2

## Done automatically

| Step | Status |
|------|--------|
| Fat JAR built | `target/surya-test-msk-publisher-1.0.0-SNAPSHOT-all.jar` (Maven Shade) |
| Copied to EC2 | `~/msk-publisher/surya-test-msk-publisher-1.0.0-SNAPSHOT-all.jar` |
| Java 17 | Installed (`amazon-corretto-headless`) |
| Network → MSK :9098 | Works (SG allows VPC) |

## Required: IAM instance profile on EC2

MSK IAM auth needs **AWS credentials** on the instance. The run failed with:

`InstanceProfileCredentialsProvider(): Failed to load credentials from IMDS`

**Fix:** Attach an **IAM instance profile** to `i-0dc8d97fef8a6dfe8` (surya-msk-publisher) whose role allows:

- `kafka-cluster:Connect` (and write) on MSK cluster `demo-cluster-1`
- Example policy resource: `arn:aws:kafka:us-east-1:ACCOUNT:cluster/demo-cluster-1/*`

Console: **EC2 → instance → Actions → Security → Modify IAM role** → choose/create role with MSK + STS access.

## Run on EC2 (after role attached)

```bash
export AWS_REGION=us-east-1
export KAFKA_BOOTSTRAP_SERVERS='<BootstrapBrokerStringSaslIam from MSK console>'
export KAFKA_TOPIC=surya-test-topic
java -jar ~/msk-publisher/surya-test-msk-publisher-1.0.0-SNAPSHOT-all.jar "optional message"
```

**SuryaTestPublisher1** (every 10s): same env, then:

```bash
java -cp ~/msk-publisher/surya-test-msk-publisher-1.0.0-SNAPSHOT-all.jar com.surya.msk.publisher.SuryaTestPublisher1
```

## Re-deploy JAR from laptop (SSH key)

```bash
scp -i YOUR_KEY.pem target/surya-test-msk-publisher-1.0.0-SNAPSHOT-all.jar ec2-user@13.220.81.78:~/msk-publisher/
```

Or **EC2 Instance Connect** (60s window): `aws ec2-instance-connect send-ssh-public-key` then `scp`.
