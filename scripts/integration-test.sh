#!/usr/bin/env bash
set -euo pipefail

SONARQUBE_URL="${SONARQUBE_URL:-http://localhost:9000}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-300}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-5}"

cleanup() {
  docker compose down -v --remove-orphans
}

dump_logs() {
  echo "::group::SonarQube Docker logs"
  docker compose logs --no-color sonarqube || true
  echo "::endgroup::"
}

trap 'status=$?; if [ "$status" -ne 0 ]; then dump_logs; fi; cleanup; exit "$status"' EXIT

echo "Building and starting SonarQube with the candidate plugin..."
docker compose up -d --build

echo "Waiting for SonarQube at ${SONARQUBE_URL}..."
deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))

while (( SECONDS < deadline )); do
  response="$(curl --silent --show-error --fail "${SONARQUBE_URL}/api/system/status" 2>/dev/null || true)"
  if [[ "$response" == *'"status":"UP"'* ]]; then
    echo "SonarQube is UP."
    break
  fi
  sleep "$POLL_INTERVAL_SECONDS"
done

if (( SECONDS >= deadline )); then
  echo "SonarQube did not become UP within ${STARTUP_TIMEOUT_SECONDS}s." >&2
  exit 1
fi

echo "Checking that the Community Branch Plugin is installed..."
plugins="$(curl --silent --show-error --fail -u admin:admin "${SONARQUBE_URL}/api/plugins/installed")"

if [[ "$plugins" != *'"key":"communityBranchPlugin"'* ]]; then
  echo "Community Branch Plugin was not reported by /api/plugins/installed." >&2
  exit 1
fi

echo "PoC smoke test passed: SonarQube started and the candidate plugin is installed."
