# shellcheck shell=bash
# Shared fixture and ALM pull request decoration tests, sourced by
# scripts/integration-test.sh.
#
# fixture_prepare builds a small git repository with known issues on a feature
# branch; it is analysed by both the ALM decoration tests and the UI check.
#
# For each ALM the plugin supports, alm_run binds a project to the recording mock
# (alm-mock service, see compose.alm-mock.yml), runs a main-branch and a pull
# request analysis of the fixture, and asserts that the plugin's decorator sent
# the expected calls (summary, inline comments on the lines SonarQube reported
# issues on, and a failed status).
#
# Relies on the caller's SONARQUBE_URL, SCANNER_IMAGE, $token and wait_for_ce_task.

FIXTURE_DIR="build/integration-test/fixture"
ALM_DIR="build/integration-test/alm"
ALM_MOCK_URL="http://alm-mock:8080"
ALM_ADMIN="${SONARQUBE_ADMIN:-admin:admin}"

alm_api() {
  # POST to the SonarQube Web API as admin; fails the test on any non-2xx answer.
  local endpoint="$1"; shift
  curl --silent --show-error --fail -u "$ALM_ADMIN" -X POST "${SONARQUBE_URL}${endpoint}" "$@" >/dev/null
}

fixture_prepare() {
  # Git fixture: a clean main branch and a feature branch that adds issues on known lines.
  local repo="$FIXTURE_DIR/repo"
  rm -rf "$FIXTURE_DIR"
  mkdir -p "$repo"
  git -C "$repo" init -q -b main
  git -C "$repo" config user.email integration-test@example.invalid
  git -C "$repo" config user.name integration-test
  printf 'def check(value):\n    return "one" if value == 1 else "other"\n\n\nprint(check(1))\n' > "$repo/app.py"
  git -C "$repo" add app.py
  git -C "$repo" commit -q -m main
  git -C "$repo" checkout -q -b feature/x
  cat > "$repo/app.py" <<'PY'
def check(value):
    if value == value:
        return "same"
    return "one" if value == 1 else "other"


print(check(1, 2))
print(undefined_name)
PY
  git -C "$repo" commit -q -am "feature with issues"
  git -C "$repo" checkout -q main
  chmod -R a+rwX "$repo"
  ALM_MOCK_HEAD_SHA="$(git -C "$repo" rev-parse feature/x)"
  ALM_MOCK_BASE_SHA="$(git -C "$repo" rev-parse main)"
  export ALM_MOCK_HEAD_SHA ALM_MOCK_BASE_SHA
}

alm_prepare() {
  # Runs before `docker compose up`: TLS material and trust store for the mock.
  local sq_version="${SONARQUBE_VERSION:-$(sed -n 's/^SONARQUBE_VERSION=//p' .env)}"
  rm -rf "$ALM_DIR"
  mkdir -p "$ALM_DIR/tls"
  : > "$ALM_DIR/requests.jsonl"

  # Throwaway CA and a certificate for Bitbucket Cloud's hard-coded hosts.
  local tls="$ALM_DIR/tls"
  openssl req -x509 -newkey rsa:2048 -nodes -keyout "$tls/ca.key" -out "$tls/ca.pem" -days 2 \
    -subj "/CN=Community Branch Plugin integration test CA" 2>/dev/null
  openssl req -newkey rsa:2048 -nodes -keyout "$tls/server.key" -out "$tls/server.csr" \
    -subj "/CN=bitbucket.org" 2>/dev/null
  printf 'subjectAltName=DNS:bitbucket.org,DNS:api.bitbucket.org\nextendedKeyUsage=serverAuth\n' > "$tls/san.ext"
  openssl x509 -req -in "$tls/server.csr" -CA "$tls/ca.pem" -CAkey "$tls/ca.key" -CAcreateserial \
    -out "$tls/server.pem" -days 2 -extfile "$tls/san.ext" 2>/dev/null
  chmod 644 "$tls/server.key"

  # SonarQube's own Java trust store plus the test CA; mounted into the SonarQube container only.
  docker run --rm --user root --entrypoint sh -v "$PWD/$tls:/tls" "sonarqube:${sq_version}" -c \
    'cp /opt/java/openjdk/lib/security/cacerts /tls/cacerts &&
     /opt/java/openjdk/bin/keytool -importcert -noprompt -alias integration-test-ca -file /tls/ca.pem \
       -keystore /tls/cacerts -storepass changeit >/dev/null && chmod 644 /tls/cacerts'

  # GitHub App private key (PKCS#1); the mock does not verify the JWT signature.
  openssl genrsa -traditional -out "$ALM_DIR/github-app.pem" 2048 2>/dev/null \
    || openssl genrsa -out "$ALM_DIR/github-app.pem" 2048 2>/dev/null
}

fixture_analyse() {
  # Usage: fixture_analyse <project> <git ref> [scanner args...]; waits for the CE task.
  local project="$1" ref="$2"; shift 2
  local out task_id status=0
  git -C "$FIXTURE_DIR/repo" checkout -q "$ref"
  out="$(docker run --rm --network host -v "$PWD/$FIXTURE_DIR/repo:/usr/src" "$SCANNER_IMAGE" \
    -Dsonar.host.url="${SONARQUBE_URL}" -Dsonar.token="$token" \
    -Dsonar.projectKey="$project" -Dsonar.sources=. "$@" 2>&1)" || status=$?
  git -C "$FIXTURE_DIR/repo" checkout -q main
  task_id="$(sed -n 's|.*api/ce/task?id=\([^[:space:]]*\).*|\1|p' <<<"$out" | tail -1)"
  if [[ "$status" -ne 0 || -z "$task_id" ]]; then
    # Show why the scan failed before the script stops.
    echo "Analysis of ${project} (${ref}) failed with exit code ${status}; scanner output:" >&2
    echo "$out" >&2
    return 1
  fi
  wait_for_ce_task "$task_id"
}

fixture_analyse_pull_request() {
  # Main branch analysis, then pull request 1 from feature/x into main.
  local project="$1"
  fixture_analyse "$project" main
  fixture_analyse "$project" feature/x -Dsonar.pullrequest.key=1 -Dsonar.pullrequest.branch=feature/x \
    -Dsonar.pullrequest.base=main
}

project_create() {
  alm_api /api/projects/create --data-urlencode "project=$1" --data-urlencode "name=$1" \
    --data-urlencode mainBranch=main
}

alm_setting_create() {
  # Creates the ALM setting for one ALM, pointing at the mock.
  case "$1" in
    gitlab)
      alm_api /api/alm_settings/create_gitlab --data-urlencode key=gitlab-mock \
        --data-urlencode "url=$ALM_MOCK_URL/api/v4" --data-urlencode personalAccessToken=glpat-mock ;;
    github)
      alm_api /api/alm_settings/create_github --data-urlencode key=github-mock \
        --data-urlencode "url=$ALM_MOCK_URL/gh" --data-urlencode appId=12345 --data-urlencode clientId=Iv1.mock \
        --data-urlencode clientSecret=mock --data-urlencode "privateKey@$ALM_DIR/github-app.pem" ;;
    azure)
      alm_api /api/alm_settings/create_azure --data-urlencode key=azure-mock \
        --data-urlencode "url=$ALM_MOCK_URL/azure" --data-urlencode personalAccessToken=azure-mock-pat ;;
    bitbucket)
      alm_api /api/alm_settings/create_bitbucket --data-urlencode key=bbs-mock \
        --data-urlencode "url=$ALM_MOCK_URL/bbs" --data-urlencode personalAccessToken=bbs-mock-pat ;;
    bitbucketcloud)
      alm_api /api/alm_settings/create_bitbucketcloud --data-urlencode key=bbcloud-mock \
        --data-urlencode clientId=mock-client --data-urlencode clientSecret=mock-secret --data-urlencode workspace=mockws ;;
  esac
}

alm_binding_set() {
  # Binds a project to the ALM setting created by alm_setting_create.
  local alm="$1" project="$2"
  case "$alm" in
    gitlab)
      alm_api /api/alm_settings/set_gitlab_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=gitlab-mock --data-urlencode repository=42 --data-urlencode monorepo=false ;;
    github)
      alm_api /api/alm_settings/set_github_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=github-mock --data-urlencode repository=acme/mock-repo \
        --data-urlencode summaryCommentEnabled=true --data-urlencode monorepo=false ;;
    azure)
      alm_api /api/alm_settings/set_azure_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=azure-mock --data-urlencode projectName=MockProject \
        --data-urlencode repositoryName=mock-repo --data-urlencode monorepo=false ;;
    bitbucket)
      alm_api /api/alm_settings/set_bitbucket_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=bbs-mock --data-urlencode repository=MOCK --data-urlencode slug=mock-repo \
        --data-urlencode monorepo=false ;;
    bitbucketcloud)
      alm_api /api/alm_settings/set_bitbucketcloud_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=bbcloud-mock --data-urlencode repository=mock-repo --data-urlencode monorepo=false ;;
  esac
}

alm_run() {
  local alm project
  for alm in ${ALM_TEST_ALMS:-gitlab github azure bitbucket bitbucketcloud}; do
    project="it-alm-${alm}"
    echo "ALM decoration: ${alm}..."
    project_create "$project"
    alm_setting_create "$alm"
    alm_binding_set "$alm" "$project"
    fixture_analyse "$project" main
    # Only record what the pull request analysis triggers.
    : > "$ALM_DIR/requests.jsonl"
    fixture_analyse "$project" feature/x -Dsonar.pullrequest.key=1 -Dsonar.pullrequest.branch=feature/x \
      -Dsonar.pullrequest.base=main
    cp "$ALM_DIR/requests.jsonl" "$ALM_DIR/requests-${alm}.jsonl"
    curl --silent --show-error --fail -u "$token:" \
      "${SONARQUBE_URL}/api/issues/search?components=${project}&pullRequest=1&issueStatuses=OPEN&ps=100" \
      > "$ALM_DIR/issues-${alm}.json"
    curl --silent --show-error --fail -u "$token:" \
      "${SONARQUBE_URL}/api/qualitygates/project_status?projectKey=${project}&pullRequest=1" \
      > "$ALM_DIR/qualitygate-${alm}.json"
    python3 scripts/integration-test/assert-decoration.py "$alm" "$ALM_DIR/requests-${alm}.jsonl" \
      "$ALM_DIR/issues-${alm}.json" "$ALM_DIR/qualitygate-${alm}.json"
  done
}

ui_project_prepare() {
  # The UI check's own project: the fixture with a failing pull request. When the
  # ALM suite ran, it is also bound to the GitHub mock, so the pull request link
  # and its ALM icon are rendered and checked too.
  local project="$1"
  project_create "$project"
  if [[ "${INTEGRATION_ALM:-true}" == "true" && " ${ALM_TEST_ALMS:-github} " == *" github "* ]]; then
    alm_binding_set github "$project"
  fi
  fixture_analyse_pull_request "$project"
}
