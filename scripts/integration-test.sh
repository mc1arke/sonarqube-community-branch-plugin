#!/usr/bin/env bash
set -euo pipefail

SONARQUBE_URL="${SONARQUBE_URL:-http://localhost:9000}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-300}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-5}"
TEST_PROJECT_KEY="community-branch-plugin-poc"
TEST_BRANCH="poc-branch"

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

echo "Creating test project..."
curl --silent --show-error --fail -u admin:admin -X POST   "${SONARQUBE_URL}/api/projects/create?project=${TEST_PROJECT_KEY}&name=${TEST_PROJECT_KEY}" >/dev/null

echo "Creating analysis token..."
token_response="$(curl --silent --show-error --fail -u admin:admin -X POST   "${SONARQUBE_URL}/api/user_tokens/generate?name=integration-test")"
token="$(printf '%s' "$token_response" | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')"

mkdir -p build/integration-test-project
cat > build/integration-test-project/sonar-project.properties <<EOF
sonar.projectKey=${TEST_PROJECT_KEY}
sonar.projectName=${TEST_PROJECT_KEY}
sonar.sources=.
sonar.exclusions=sonar-project.properties
EOF
cat > build/integration-test-project/example.js <<'EOF'
function hello(name) {
  return "Hello " + name;
}
console.log(hello("branch"));
EOF

echo "Running a real branch analysis for '${TEST_BRANCH}'..."
docker run --rm --network host   -v "$PWD/build/integration-test-project:/usr/src"   sonarsource/sonar-scanner-cli   -Dsonar.host.url="${SONARQUBE_URL}"   -Dsonar.token="$token"   -Dsonar.branch.name="${TEST_BRANCH}"

echo "Waiting for branch analysis to be processed..."
deadline=$((SECONDS + 120))
while (( SECONDS < deadline )); do
  branches="$(curl --silent --show-error --fail -u "$token:"     "${SONARQUBE_URL}/api/project_branches/list?project=${TEST_PROJECT_KEY}")"
  if [[ "$branches" == *"\"name\":\"${TEST_BRANCH}\""* ]]; then
    echo "Branch '${TEST_BRANCH}' is visible through the SonarQube API."
    echo "PoC integration test passed: the candidate plugin supports branch analysis."
    exit 0
  fi
  sleep "$POLL_INTERVAL_SECONDS"
done

echo "Branch '${TEST_BRANCH}' was not visible through /api/project_branches/list." >&2
exit 1
