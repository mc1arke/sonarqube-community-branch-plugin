#!/bin/sh

set -eu

plugin_name="sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar"
sonarqube_home="${SONARQUBE_HOME:-/opt/sonarqube}"
plugin_source="${sonarqube_home}/lib/community-branch-plugin/${plugin_name}"
plugin_directory="${sonarqube_home}/extensions/plugins"
disabled_directory="${sonarqube_home}/extensions/disabled-plugins"
plugin_target="${plugin_directory}/${plugin_name}"

mkdir -p "${plugin_directory}" "${disabled_directory}"

for installed_plugin in \
  "${plugin_directory}/sonarqube-community-branch-plugin.jar" \
  "${plugin_directory}"/sonarqube-community-branch-plugin-*.jar; do
  [ -f "${installed_plugin}" ] || continue
  [ "${installed_plugin}" = "${plugin_target}" ] ||
    mv "${installed_plugin}" "${disabled_directory}/$(basename "${installed_plugin}").disabled"
done

plugin_temporary="${plugin_target}.tmp"
cp "${plugin_source}" "${plugin_temporary}"
chmod 644 "${plugin_temporary}"
mv "${plugin_temporary}" "${plugin_target}"

exec "${sonarqube_home}/docker/entrypoint.sh" "$@"
