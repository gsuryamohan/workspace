#!/usr/bin/env bash
# Run on surya-msk-publisher EC2 after JAR is in ~/msk-publisher/
set -e
export AWS_REGION="${AWS_REGION:-us-east-1}"
export KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:?Set KAFKA_BOOTSTRAP_SERVERS (IAM 9098)}"
export KAFKA_TOPIC="${KAFKA_TOPIC:-surya-test-topic}"
JAR="$HOME/msk-publisher/surya-test-msk-publisher-1.0.0-SNAPSHOT-all.jar"
exec java -jar "$JAR" "$@"
