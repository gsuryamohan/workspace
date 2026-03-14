#!/usr/bin/env bash
# Deploy Fresh Samosa React app to surya-service Tomcat webapps.
# Usage: ./deploy-fresh-samosa.sh [path-to-surya-service-key.pem]
#
# Prerequisites:
# - Port 8080 whitelisted in EC2 security group (Inbound: Custom TCP 8080)
# - Tomcat installed on surya-service
# - SSH key at ~/.ssh/surya-service-key.pem (or pass as arg)

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRESH_SAMOSA_DIR="$(cd "$SCRIPT_DIR/../fresh-samosa" && pwd)"
HOST="16.145.70.106"
USER="ec2-user"
KEY="${1:-$HOME/.ssh/surya-service-key.pem}"
WEBAPPS="/opt/tomcat/webapps"
REMOTE_TMP="/home/ec2-user/fresh-samosa-deploy"

if [[ ! -d "$FRESH_SAMOSA_DIR" ]]; then
  echo "ERROR: fresh-samosa not found at $FRESH_SAMOSA_DIR"
  exit 1
fi
if [[ ! -f "$KEY" ]]; then
  echo "SSH key not found: $KEY"
  echo "Usage: $0 [path-to-.pem]"
  exit 1
fi

echo "Building Fresh Samosa for Tomcat (base=/fresh-samosa/)..."
(cd "$FRESH_SAMOSA_DIR" && npm run build:tomcat)

if [[ ! -d "$FRESH_SAMOSA_DIR/dist" ]]; then
  echo "ERROR: Build failed - no dist/ folder"
  exit 1
fi

echo "Syncing dist/ to ${USER}@${HOST}:${REMOTE_TMP}..."
ssh -i "$KEY" -o StrictHostKeyChecking=accept-new "${USER}@${HOST}" "rm -rf $REMOTE_TMP && mkdir -p $REMOTE_TMP"
scp -i "$KEY" -o StrictHostKeyChecking=accept-new -r "$FRESH_SAMOSA_DIR/dist/"* "${USER}@${HOST}:${REMOTE_TMP}/"

echo "Copying to Tomcat webapps/fresh-samosa..."
ssh -i "$KEY" -o StrictHostKeyChecking=accept-new "${USER}@${HOST}" "sudo mkdir -p $WEBAPPS/fresh-samosa && sudo cp -r $REMOTE_TMP/* $WEBAPPS/fresh-samosa/ && sudo chown -R tomcat:tomcat $WEBAPPS/fresh-samosa && rm -rf $REMOTE_TMP"

echo ""
echo "=== Fresh Samosa deployed to Tomcat ==="
echo "URL: http://${HOST}:8080/fresh-samosa/"
echo ""
echo "If the app does not load, ensure port 8080 is whitelisted in the EC2 security group:"
echo "  EC2 -> Security Groups -> Inbound -> Add rule: Custom TCP, Port 8080, Source: 0.0.0.0/0 (or My IP)"
echo ""
