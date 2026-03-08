#!/usr/bin/env bash
# Deploy SimpleMath service JAR to surya-service and restart.
# Prereqs: SSH key for 'surya-service-key' (e.g. ~/.ssh/surya-service-key.pem) and access to 16.145.70.106

set -e
HOST="16.145.70.106"
USER="ec2-user"
KEY="${1:-$HOME/.ssh/surya-service-key.pem}"
JAR="target/simplemath-service-0.0.1-SNAPSHOT.jar"
REMOTE_DIR="/home/ec2-user"
REMOTE_JAR="simplemath-service.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Build JAR first: ./mvnw package -DskipTests"
  exit 1
fi

if [[ ! -f "$KEY" ]]; then
  echo "SSH key not found: $KEY"
  echo "Usage: $0 [path-to-.pem]"
  exit 1
fi

echo "Copying JAR to ${USER}@${HOST}..."
scp -i "$KEY" -o StrictHostKeyChecking=no "$JAR" "${USER}@${HOST}:${REMOTE_DIR}/${REMOTE_JAR}"

echo "Stopping old process (if any), starting new JAR..."
ssh -i "$KEY" -o StrictHostKeyChecking=no "${USER}@${HOST}" "pkill -f ${REMOTE_JAR} || true; sleep 2; nohup java -jar ${REMOTE_DIR}/${REMOTE_JAR} > ${REMOTE_DIR}/simplemath.log 2>&1 & sleep 2; pgrep -f ${REMOTE_JAR} && echo 'Service started.' || echo 'Start failed - check ${REMOTE_DIR}/simplemath.log'"

echo "Done. Test: curl -s -X POST http://${HOST}:8080/api/v1/math/add -H 'Content-Type: application/json' -d '{\"a\":2,\"b\":3}'"
