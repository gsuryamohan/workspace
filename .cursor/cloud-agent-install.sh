#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT}/simplemath-service"
./mvnw -B dependency:go-offline test

cd "${ROOT}/fresh-samosa"
npm ci
