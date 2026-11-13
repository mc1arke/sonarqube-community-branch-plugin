# shellcheck shell=bash
# ALM pull request decoration tests, sourced by scripts/integration-test.sh.
#
# For each ALM the plugin supports, binds a project to the recording mock
# (alm-mock service, see compose.alm-mock.yml), runs a main-branch and a pull
# request analysis of a small git fixture with known issues, and asserts that the
# plugin's decorator sent the expected calls (summary, inline comments on the
# lines SonarQube reported issues on, and a failed status).
#
# Relies on the caller's SONARQUBE_URL, SCANNER_IMAGE, $token and wait_for_ce_task.

ALM_DIR="build/integration-test/alm"
ALM_MOCK_URL="http://alm-mock:8080"
ALM_ADMIN="${SONARQUBE_ADMIN:-admin:admin}"

alm_api() {
  # POST to the SonarQube Web API as admin; fails the test on any non-2xx answer.
  local endpoint="$1"; shift
  curl --silent --show-error --fail -u "$ALM_ADMIN" -X POST "${SONARQUBE_URL}${endpoint}" "$@" >/dev/null
}

alm_prepare() {
  # Runs before `docker compose up`: fixture repository, TLS material and trust store.
  local sq_version="${SONARQUBE_VERSION:-$(sed -n 's/^SONARQUBE_VERSION=//p' .env)}"
  rm -rf "$ALM_DIR"
  mkdir -p "$ALM_DIR/repo" "$ALM_DIR/tls"
  : > "$ALM_DIR/requests.jsonl"

  # Git fixture: a clean main branch and a feature branch that adds issues on known lines.
  git -C "$ALM_DIR/repo" init -q -b main
  git -C "$ALM_DIR/repo" config user.email integration-test@example.invalid
  git -C "$ALM_DIR/repo" config user.name integration-test
  printf 'def check(value):\n    return "one" if value == 1 else "other"\n\n\nprint(check(1))\n' > "$ALM_DIR/repo/app.py"
  git -C "$ALM_DIR/repo" add app.py
  git -C "$ALM_DIR/repo" commit -q -m main
  git -C "$ALM_DIR/repo" checkout -q -b feature/x
  cat > "$ALM_DIR/repo/app.py" <<'PY'
def check(value):
    if value == value:
        return "same"
    return "one" if value == 1 else "other"


print(check(1, 2))
print(undefined_name)
PY
  git -C "$ALM_DIR/repo" commit -q -am "feature with issues"
  git -C "$ALM_DIR/repo" checkout -q main
  chmod -R a+rwX "$ALM_DIR/repo"
  ALM_MOCK_HEAD_SHA="$(git -C "$ALM_DIR/repo" rev-parse feature/x)"
  ALM_MOCK_BASE_SHA="$(git -C "$ALM_DIR/repo" rev-parse main)"
  export ALM_MOCK_HEAD_SHA ALM_MOCK_BASE_SHA

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

alm_analyse() {
  # Usage: alm_analyse <project> <git ref> [scanner args...]; waits for the CE task.
  local project="$1" ref="$2"; shift 2
  local out task_id
  git -C "$ALM_DIR/repo" checkout -q "$ref"
  out="$(docker run --rm --network host -v "$PWD/$ALM_DIR/repo:/usr/src" "$SCANNER_IMAGE" \
    -Dsonar.host.url="${SONARQUBE_URL}" -Dsonar.token="$token" \
    -Dsonar.projectKey="$project" -Dsonar.sources=. "$@" 2>&1)"
  git -C "$ALM_DIR/repo" checkout -q main
  task_id="$(sed -n 's|.*api/ce/task?id=\([^[:space:]]*\).*|\1|p' <<<"$out" | tail -1)"
  if [[ -z "$task_id" ]]; then
    echo "$out" >&2
    return 1
  fi
  wait_for_ce_task "$task_id"
}

alm_bind() {
  # Creates the project, the ALM setting pointing at the mock, and the project binding.
  local alm="$1" project="$2"
  alm_api /api/projects/create --data-urlencode "project=$project" --data-urlencode "name=$project" \
    --data-urlencode mainBranch=main
  case "$alm" in
    gitlab)
      alm_api /api/alm_settings/create_gitlab --data-urlencode key=gitlab-mock \
        --data-urlencode "url=$ALM_MOCK_URL/api/v4" --data-urlencode personalAccessToken=glpat-mock
      alm_api /api/alm_settings/set_gitlab_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=gitlab-mock --data-urlencode repository=42 --data-urlencode monorepo=false ;;
    github)
      alm_api /api/alm_settings/create_github --data-urlencode key=github-mock \
        --data-urlencode "url=$ALM_MOCK_URL/gh" --data-urlencode appId=12345 --data-urlencode clientId=Iv1.mock \
        --data-urlencode clientSecret=mock --data-urlencode "privateKey@$ALM_DIR/github-app.pem"
      alm_api /api/alm_settings/set_github_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=github-mock --data-urlencode repository=acme/mock-repo \
        --data-urlencode summaryCommentEnabled=true --data-urlencode monorepo=false ;;
    azure)
      alm_api /api/alm_settings/create_azure --data-urlencode key=azure-mock \
        --data-urlencode "url=$ALM_MOCK_URL/azure" --data-urlencode personalAccessToken=azure-mock-pat
      alm_api /api/alm_settings/set_azure_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=azure-mock --data-urlencode projectName=MockProject \
        --data-urlencode repositoryName=mock-repo --data-urlencode monorepo=false ;;
    bitbucket)
      alm_api /api/alm_settings/create_bitbucket --data-urlencode key=bbs-mock \
        --data-urlencode "url=$ALM_MOCK_URL/bbs" --data-urlencode personalAccessToken=bbs-mock-pat
      alm_api /api/alm_settings/set_bitbucket_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=bbs-mock --data-urlencode repository=MOCK --data-urlencode slug=mock-repo \
        --data-urlencode monorepo=false ;;
    bitbucketcloud)
      alm_api /api/alm_settings/create_bitbucketcloud --data-urlencode key=bbcloud-mock \
        --data-urlencode clientId=mock-client --data-urlencode clientSecret=mock-secret --data-urlencode workspace=mockws
      alm_api /api/alm_settings/set_bitbucketcloud_binding --data-urlencode "project=$project" \
        --data-urlencode almSetting=bbcloud-mock --data-urlencode repository=mock-repo --data-urlencode monorepo=false ;;
  esac
}

alm_run() {
  local alm project
  for alm in ${ALM_TEST_ALMS:-gitlab github azure bitbucket bitbucketcloud}; do
    project="it-alm-${alm}"
    echo "ALM decoration: ${alm}..."
    alm_bind "$alm" "$project"
    alm_analyse "$project" main
    # Only record what the pull request analysis triggers.
    : > "$ALM_DIR/requests.jsonl"
    alm_analyse "$project" feature/x -Dsonar.pullrequest.key=1 -Dsonar.pullrequest.branch=feature/x \
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
