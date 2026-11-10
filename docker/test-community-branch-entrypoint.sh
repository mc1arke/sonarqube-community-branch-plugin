#!/bin/sh

set -eu

repository_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
test_root=$(mktemp -d)
trap 'rm -rf "${test_root}"' EXIT HUP INT TERM

plugin_version="26.8.0-SNAPSHOT"
plugin_name="sonarqube-community-branch-plugin-${plugin_version}.jar"
plugin_source="${test_root}/lib/community-branch-plugin/${plugin_name}"
plugin_directory="${test_root}/extensions/plugins"
disabled_directory="${test_root}/extensions/disabled-plugins"

mkdir -p "$(dirname "${plugin_source}")" "${plugin_directory}" "${test_root}/docker"
printf '%s\n' '#!/bin/sh' 'exit 0' > "${test_root}/docker/entrypoint.sh"
chmod +x "${test_root}/docker/entrypoint.sh"

printf '%s\n' 'current plugin' > "${plugin_source}"
printf '%s\n' 'legacy generic plugin' > "${plugin_directory}/sonarqube-community-branch-plugin.jar"
printf '%s\n' 'legacy versioned plugin' > "${plugin_directory}/sonarqube-community-branch-plugin-26.7.0.jar"
printf '%s\n' 'legacy renamed plugin' > "${plugin_directory}/sonarqube-community-branch-plugin_old.jar"

# A missing plugin source fails before any installed plugin is disabled.
if SONARQUBE_HOME="${test_root}" PLUGIN_VERSION="missing" \
  sh "${repository_root}/docker/community-branch-entrypoint.sh" 2>/dev/null; then
  exit 1
fi
test -f "${plugin_directory}/sonarqube-community-branch-plugin.jar"
test ! -e "${disabled_directory}/sonarqube-community-branch-plugin.jar.disabled"

SONARQUBE_HOME="${test_root}" PLUGIN_VERSION="${plugin_version}" \
  sh "${repository_root}/docker/community-branch-entrypoint.sh"

cmp "${plugin_source}" "${plugin_directory}/${plugin_name}"
test ! -e "${plugin_directory}/sonarqube-community-branch-plugin.jar"
test ! -e "${plugin_directory}/sonarqube-community-branch-plugin-26.7.0.jar"
test -f "${disabled_directory}/sonarqube-community-branch-plugin.jar.disabled"
test -f "${disabled_directory}/sonarqube-community-branch-plugin-26.7.0.jar.disabled"
test -f "${disabled_directory}/sonarqube-community-branch-plugin_old.jar.disabled"

# A restart refreshes the managed copy without duplicating or disabling it.
printf '%s\n' 'current plugin after restart' > "${plugin_source}"
SONARQUBE_HOME="${test_root}" PLUGIN_VERSION="${plugin_version}" \
  sh "${repository_root}/docker/community-branch-entrypoint.sh"

cmp "${plugin_source}" "${plugin_directory}/${plugin_name}"
test "$(find "${plugin_directory}" -name 'sonarqube-community-branch-plugin*.jar' | wc -l | tr -d ' ')" = "1"

# A fixed plugin name, as in the release image, disables versioned copies and is replaced on upgrade.
fixed_name="sonarqube-community-branch-plugin.jar"
fixed_source="$(dirname "${plugin_source}")/${fixed_name}"
for release_plugin in 'release plugin' 'release plugin after upgrade'; do
  printf '%s\n' "${release_plugin}" > "${fixed_source}"
  SONARQUBE_HOME="${test_root}" PLUGIN_VERSION="${plugin_version}" PLUGIN_JAR_NAME="${fixed_name}" \
    sh "${repository_root}/docker/community-branch-entrypoint.sh"

  cmp "${fixed_source}" "${plugin_directory}/${fixed_name}"
  test ! -e "${plugin_directory}/${plugin_name}"
  test -f "${disabled_directory}/${plugin_name}.disabled"
  test "$(find "${plugin_directory}" -name 'sonarqube-community-branch-plugin*.jar' | wc -l | tr -d ' ')" = "1"
done

# Both image variants must install the immutable source and use the migration entrypoint.
for dockerfile in Dockerfile release.Dockerfile; do
  grep -Fq '/opt/sonarqube/lib/community-branch-plugin/' "${repository_root}/${dockerfile}"
  grep -Fq 'docker/community-branch-entrypoint.sh' "${repository_root}/${dockerfile}"
  grep -Fq 'ENTRYPOINT ["/opt/sonarqube/docker/community-branch-entrypoint.sh"]' "${repository_root}/${dockerfile}"
done

echo "All checks passed"
