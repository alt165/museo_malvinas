#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

: "${NVD_API_KEY:?NVD_API_KEY must be supplied through a secret, never committed}"
data_dir=${DEPENDENCY_CHECK_DATA_DIR:-$REPO_DIR/dependency-check-data}
mkdir -p "$data_dir"

cd "$REPO_DIR/backend"
mvn --batch-mode org.owasp:dependency-check-maven:13.0.0:check \
  -DnvdApiKey="$NVD_API_KEY" \
  -DdataDirectory="$data_dir" \
  -Dformats=HTML,JSON,SARIF \
  -DfailBuildOnCVSS=7.0 \
  -DsuppressionFile="$REPO_DIR/ops/dependency-check-suppressions.xml"

