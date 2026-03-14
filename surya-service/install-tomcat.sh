#!/usr/bin/env bash
#
# Install Apache Tomcat on EC2 (surya-service) - compatible with existing Java.
# Detects Java version and installs: Java 17+ -> Tomcat 11, Java 11+ -> Tomcat 10.1, Java 8+ -> Tomcat 9.
#
# Run on the EC2 instance: sudo bash install-tomcat.sh
#

set -e

TOMCAT_USER=tomcat
TOMCAT_GROUP=tomcat
INSTALL_BASE=/opt/tomcat
# Apache mirror for downloads
APACHE_MIRROR="${APACHE_MIRROR:-https://dlcdn.apache.org}"

# --- Detect Java version ---
detect_java_version() {
  if ! command -v java &>/dev/null; then
    echo "ERROR: Java is not installed. Install Java first (e.g. openjdk-11 or openjdk-17)."
    exit 1
  fi

  local version_output
  version_output=$(java -version 2>&1)
  echo "Detected Java: $version_output" 1>&2

  # Parse major version from output like "openjdk version \"11.0.x\"" or "java version \"1.8.0_xxx\""
  local major
  if [[ "$version_output" =~ \"([0-9]+)\. ]]; then
    major="${BASH_REMATCH[1]}"
  else
    echo "ERROR: Could not parse Java version."
    exit 1
  fi

  # Java 8 and earlier report as 1.8, 1.7, etc.
  if [[ "$version_output" =~ \"1\.([0-9]+)\. ]]; then
    major="${BASH_REMATCH[1]}"
  fi

  echo "Java major version: $major" 1>&2
  echo "$major"
}

# --- Select Tomcat version and set globals DOWNLOAD_URL, TOMCAT_VERSION ---
select_tomcat() {
  local java_major="$1"

  # Tomcat 10.1.x = Java 11+, Tomcat 9.0.x = Java 8+
  if [[ "$java_major" -ge 11 ]]; then
    TOMCAT_VERSION="10.1.52"
    TOMCAT_ARCHIVE="tomcat-10.1.52"
  elif [[ "$java_major" -ge 8 ]]; then
    TOMCAT_VERSION="9.0.115"
    TOMCAT_ARCHIVE="tomcat-9.0.115"
  else
    echo "ERROR: Java $java_major is not supported. Tomcat requires Java 8 or later."
    exit 1
  fi

  local family="${TOMCAT_VERSION%%.*}"
  DOWNLOAD_URL="${APACHE_MIRROR}/tomcat/tomcat-${family}/v${TOMCAT_VERSION}/bin/apache-${TOMCAT_ARCHIVE}.tar.gz"
  echo "Selected Tomcat ${TOMCAT_VERSION} (compatible with Java ${java_major})"
}

# --- Main install ---
main() {
  echo "=== Tomcat installer for surya-service EC2 ==="
  [[ "$(id -u)" -eq 0 ]] || { echo "Run with sudo."; exit 1; }

  local java_major
  java_major=$(detect_java_version)

  select_tomcat "$java_major"

  echo "Downloading $DOWNLOAD_URL ..."
  local tmpdir
  tmpdir=$(mktemp -d)
  trap "rm -rf '$tmpdir'" EXIT
  curl -fsSL -o "$tmpdir/tomcat.tar.gz" "$DOWNLOAD_URL"

  if ! getent group "$TOMCAT_GROUP" &>/dev/null; then
    groupadd -r "$TOMCAT_GROUP"
  fi
  if ! id "$TOMCAT_USER" &>/dev/null; then
    echo "Creating user $TOMCAT_USER ..."
    useradd -r -s /bin/false -d "$INSTALL_BASE" -g "$TOMCAT_GROUP" "$TOMCAT_USER"
  fi

  echo "Installing to $INSTALL_BASE ..."
  mkdir -p "$INSTALL_BASE"
  tar -xzf "$tmpdir/tomcat.tar.gz" -C "$INSTALL_BASE" --strip-components=1

  chown -R "$TOMCAT_USER:$TOMCAT_GROUP" "$INSTALL_BASE"
  chmod +x "$INSTALL_BASE/bin/"*.sh
  # Remove default webapps we may not need (optional; comment out to keep)
  # rm -rf "$INSTALL_BASE/webapps/docs" "$INSTALL_BASE/webapps/examples" "$INSTALL_BASE/webapps/manager" "$INSTALL_BASE/webapps/host-manager"

  # systemd service
  cat > /etc/systemd/system/tomcat.service << EOF
[Unit]
Description=Apache Tomcat Web Server
After=network.target

[Service]
Type=forking
User=$TOMCAT_USER
Group=$TOMCAT_GROUP
Environment="JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))"
Environment="CATALINA_HOME=$INSTALL_BASE"
Environment="CATALINA_BASE=$INSTALL_BASE"
ExecStart=$INSTALL_BASE/bin/startup.sh
ExecStop=$INSTALL_BASE/bin/shutdown.sh
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

  systemctl daemon-reload
  systemctl enable tomcat
  systemctl start tomcat

  # Firewall (optional; uncomment if you use firewalld/ufw)
  # if command -v firewall-cmd &>/dev/null; then
  #   firewall-cmd --permanent --add-port=8080/tcp
  #   firewall-cmd --reload
  # fi
  # if command -v ufw &>/dev/null && ufw status | grep -q active; then
  #   ufw allow 8080/tcp
  #   ufw reload
  # fi

  echo ""
  echo "=== Tomcat $TOMCAT_VERSION installed and started ==="
  echo "  Install path: $INSTALL_BASE"
  echo "  URL:          http://$(curl -s --connect-timeout 2 http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo 'localhost'):8080"
  echo "  Service:      systemctl status tomcat"
  echo "  Logs:         journalctl -u tomcat -f"
}

main "$@"
