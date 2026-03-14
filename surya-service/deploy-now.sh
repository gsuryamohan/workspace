#!/usr/bin/env bash
# Push Instance Connect key then immediately SCP JAR and restart. Run from workspace root.
set -e
KEYDIR="$(dirname "$0")/.ec2-connect-keys"
HOST="16.145.70.106"
JAR="$(dirname "$0")/../simplemath-service/target/simplemath-service-0.0.1-SNAPSHOT.jar"
PUBKEY=$(cat "$KEYDIR/temp_key.pub")
aws ec2-instance-connect send-ssh-public-key --instance-id i-096aed3497e86e241 --instance-os-user ec2-user --availability-zone us-west-2b --ssh-public-key "$PUBKEY" --output text --query Success
echo "Key pushed; deploying within 60s..."
scp -i "$KEYDIR/temp_key" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15 "$JAR" "ec2-user@${HOST}:/home/ec2-user/simplemath-service.jar"
ssh -i "$KEYDIR/temp_key" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15 "ec2-user@${HOST}" "pkill -f simplemath-service.jar || true; sleep 2; nohup java -jar /home/ec2-user/simplemath-service.jar > /home/ec2-user/simplemath.log 2>&1 & sleep 2; pgrep -f simplemath-service.jar && echo OK || echo FAIL"
echo "Done."
