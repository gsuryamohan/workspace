#!/usr/bin/env bash
# Copy install-tomcat.sh to surya-service EC2 and run it. Port 8080 must be allowed in the instance security group.
# Usage: ./deploy-tomcat.sh [path-to-surya-service-key.pem]

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_SCRIPT="${SCRIPT_DIR}/install-tomcat.sh"
HOST="16.145.70.106"
USER="ec2-user"
KEY="${1:-$HOME/.ssh/surya-service-key.pem}"

if [[ ! -f "$INSTALL_SCRIPT" ]]; then
  echo "Not found: $INSTALL_SCRIPT"
  exit 1
fi
if [[ ! -f "$KEY" ]]; then
  echo "SSH key not found: $KEY"
  echo "Usage: $0 [path-to-.pem]"
  exit 1
fi

echo "Copying install-tomcat.sh to ${USER}@${HOST}..."
scp -i "$KEY" -o StrictHostKeyChecking=accept-new "$INSTALL_SCRIPT" "${USER}@${HOST}:/tmp/install-tomcat.sh"

echo "Running Tomcat install on instance (sudo)..."
ssh -i "$KEY" -o StrictHostKeyChecking=accept-new "${USER}@${HOST}" "sudo bash /tmp/install-tomcat.sh"

echo ""
echo "Tomcat is installed. Open: http://${HOST}:8080"
