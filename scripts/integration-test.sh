#!/usr/bin/env bash
set -euo pipefail

# End-to-end release-candidate test for the Community Branch Plugin.
#
# This test deliberately exercises SonarQube from the outside, as a user would:
#   1. build/start SonarQube with the candidate plugin;
#   2. verify the plugin is installed;
#   3. run a normal main-branch analysis;
#   4. run a named branch analysis and verify its Compute Engine task completes;
#   5. run a pull-request analysis and verify its Compute Engine task completes;
#   6. verify SonarQube computed a quality gate for branch and PR analyses.
#
# Waiting for the Compute Engine (CE) task is important. The scanner reports
# "ANALYSIS SUCCESSFUL" after uploading the report, before SonarQube has actually
# processed it. A broken plugin can therefore make the CE task fail even though
# the scanner itself exits successfully.
#
# The scanner image is pinned so a new scanner release cannot silently change
# the behaviour of this release-candidate test. Override SCANNER_IMAGE when
# intentionally testing another scanner version.

SONARQUBE_URL="${SONARQUBE_URL:-http://localhost:9000}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-300}"
CE_TIMEOUT_SECONDS="${CE_TIMEOUT_SECONDS:-120}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-5}"
SCANNER_IMAGE="${SCANNER_IMAGE:-sonarsource/sonar-scanner-cli:12.2.0.4256_8.1.0}"

TEST_PROJECT_KEY="community-branch-plugin-poc"
TEST_BRANCH="poc-branch"
TEST_PR_KEY="1"
TEST_PR_BRANCH="feature/x"
TEST_PR_BASE="main"

cleanup() {
  # Remove containers, networks and volumes so repeated/local runs start clean.
  docker compose down -v --remove-orphans
}

dump_logs() {
  # Keep the server-side failure visible in GitHub Actions when a test fails.
  echo "::group::SonarQube Docker logs"
  docker compose logs --no-color sonarqube || true
  echo "::endgroup::"
}

trap 'status=$?; if [ "$status" -ne 0 ]; then dump_logs; fi; cleanup; exit "$status"' EXIT

wait_for_ce_task() {
  # Wait for the exact Compute Engine task produced by one scanner invocation.
  # SUCCESS proves SonarQube processed the uploaded report. FAILED/CANCELED
  # catches server/plugin failures that the scanner exit code cannot detect.
  local task_id="$1"
  local status
  local deadline=$((SECONDS + CE_TIMEOUT_SECONDS))

  if [[ -z "$task_id" ]]; then
    echo "Could not extract a Compute Engine task id from scanner output." >&2
    return 1
  fi

  echo "Waiting for Compute Engine task ${task_id}..."
  while (( SECONDS < deadline )); do
    status="$(curl --silent --show-error --fail -u "$token:"       "${SONARQUBE_URL}/api/ce/task?id=${task_id}"       | python3 -c 'import json,sys; print(json.load(sys.stdin)["task"]["status"])')"

    case "$status" in
      SUCCESS)
        echo "Compute Engine task ${task_id} completed successfully."
        return 0
        ;;
      FAILED|CANCELED)
        echo "Compute Engine task ${task_id} ended in ${status}." >&2
        return 1
        ;;
    esac

    sleep "$POLL_INTERVAL_SECONDS"
  done

  echo "Compute Engine task ${task_id} did not finish within ${CE_TIMEOUT_SECONDS}s." >&2
  return 1
}

assert_quality_gate() {
  # A branch/PR existing in SonarQube does not prove its analysis succeeded.
  # A computed quality-gate status confirms the CE task produced an analysis.
  # Both OK and ERROR are valid test outcomes: ERROR means the analysed code
  # failed its configured gate, not that the plugin/integration failed.
  local qualifier="$1"
  local gate

  gate="$(curl --silent --show-error --fail -u "$token:"     "${SONARQUBE_URL}/api/qualitygates/project_status?projectKey=${TEST_PROJECT_KEY}&${qualifier}"     | python3 -c 'import json,sys; print(json.load(sys.stdin)["projectStatus"]["status"])')"

  if [[ "$gate" != "OK" && "$gate" != "ERROR" ]]; then
    echo "No quality gate computed for ${qualifier} (got '${gate}')." >&2
    return 1
  fi

  echo "Quality gate for ${qualifier}: ${gate}"
}

analyse() {
  # The official scanner image stores .scannerwork/report-task.txt inside its
  # container. Capture the CE task URL printed by the scanner instead, extract
  # the task id, and wait for that exact server-side task to complete.
  local out
  local task_id

  out="$(docker run --rm --network host     -v "$PWD/build/integration-test-project:/usr/src"     "$SCANNER_IMAGE"     -Dsonar.host.url="${SONARQUBE_URL}"     -Dsonar.token="$token"     "$@" | tee /dev/stderr)"

  task_id="$(sed -n 's|.*api/ce/task?id=\([^[:space:]]*\).*|\1|p' <<<"$out" | tail -1)"
  wait_for_ce_task "$task_id"
}

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

# Create a disposable project and token used by all three analysis scenarios.
echo "Creating test project..."
curl --silent --show-error --fail -u admin:admin -X POST   "${SONARQUBE_URL}/api/projects/create?project=${TEST_PROJECT_KEY}&name=${TEST_PROJECT_KEY}" >/dev/null

echo "Creating analysis token..."
token_response="$(curl --silent --show-error --fail -u admin:admin -X POST   "${SONARQUBE_URL}/api/user_tokens/generate?name=integration-test")"
token="$(printf '%s' "$token_response"   | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')"

# Keep the fixture tiny. Its purpose is to exercise the scanner/plugin protocol,
# not to test a particular language analyser or quality profile.
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
console.log(hello("integration-test"));
EOF

echo "1/3: Running main-branch analysis..."
analyse

echo "2/3: Running named branch analysis '${TEST_BRANCH}'..."
analyse -Dsonar.branch.name="${TEST_BRANCH}"
assert_quality_gate "branch=${TEST_BRANCH}"

echo "3/3: Running pull-request analysis '${TEST_PR_KEY}'..."
analyse   -Dsonar.pullrequest.key="${TEST_PR_KEY}"   -Dsonar.pullrequest.branch="${TEST_PR_BRANCH}"   -Dsonar.pullrequest.base="${TEST_PR_BASE}"
assert_quality_gate "pullRequest=${TEST_PR_KEY}"

echo "PoC integration test passed:"
echo "  - main-branch analysis completed in the Compute Engine"
echo "  - named branch analysis completed and produced a quality gate"
echo "  - pull-request analysis completed and produced a quality gate"
