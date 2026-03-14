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

echo "Installing/restarting systemd service..."
ssh -i "$KEY" -o StrictHostKeyChecking=no "${USER}@${HOST}" 'sudo bash -s' << 'REMOTE'
REMOTE_DIR="/home/ec2-user"
REMOTE_JAR="simplemath-service.jar"
UNIT_FILE="/etc/systemd/system/simplemath-service.service"
if [[ ! -f "$UNIT_FILE" ]]; then
  cat > "$UNIT_FILE" << EOF
[Unit]
Description=SimpleMath API
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=$REMOTE_DIR
ExecStart=/usr/bin/java -jar $REMOTE_DIR/$REMOTE_JAR
Restart=on-failure
RestartSec=5
StandardOutput=append:$REMOTE_DIR/simplemath.log
StandardError=append:$REMOTE_DIR/simplemath.log

[Install]
WantedBy=multi-user.target
EOF
  systemctl daemon-reload
  systemctl enable simplemath-service
fi
systemctl restart simplemath-service
sleep 2
systemctl is-active simplemath-service && echo "Service started." || echo "Start failed - check journalctl -u simplemath-service"
REMOTE

echo "Done. Root: curl -s http://${HOST}:8080/"
echo "Math:   curl -s -X POST http://${HOST}:8080/api/v1/math/add -H 'Content-Type: application/json' -d '{\"a\":2,\"b\":3}'"
