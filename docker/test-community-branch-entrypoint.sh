#!/bin/sh

set -eu

repository_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
test_root=$(mktemp -d)
trap 'rm -rf "${test_root}"' EXIT HUP INT TERM

plugin_version="26.9.0-SNAPSHOT"
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

SONARQUBE_HOME="${test_root}" PLUGIN_VERSION="${plugin_version}" \
  sh "${repository_root}/docker/community-branch-entrypoint.sh"

cmp "${plugin_source}" "${plugin_directory}/${plugin_name}"
test ! -e "${plugin_directory}/sonarqube-community-branch-plugin.jar"
test ! -e "${plugin_directory}/sonarqube-community-branch-plugin-26.7.0.jar"
test -f "${disabled_directory}/sonarqube-community-branch-plugin.jar.disabled"
test -f "${disabled_directory}/sonarqube-community-branch-plugin-26.7.0.jar.disabled"

# A restart refreshes the managed copy without duplicating or disabling it.
printf '%s\n' 'current plugin after restart' > "${plugin_source}"
SONARQUBE_HOME="${test_root}" PLUGIN_VERSION="${plugin_version}" \
  sh "${repository_root}/docker/community-branch-entrypoint.sh"

cmp "${plugin_source}" "${plugin_directory}/${plugin_name}"
test "$(find "${plugin_directory}" -name 'sonarqube-community-branch-plugin*.jar' | wc -l | tr -d ' ')" = "1"

# Both image variants must install the immutable source and use the migration entrypoint.
for dockerfile in Dockerfile release.Dockerfile; do
  grep -Fq '/opt/sonarqube/lib/community-branch-plugin/' "${repository_root}/${dockerfile}"
  grep -Fq 'docker/community-branch-entrypoint.sh' "${repository_root}/${dockerfile}"
  grep -Fq 'ENTRYPOINT ["/opt/sonarqube/docker/community-branch-entrypoint.sh"]' "${repository_root}/${dockerfile}"
done
