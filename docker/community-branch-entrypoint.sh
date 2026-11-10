#!/bin/sh

set -eu

plugin_name="${PLUGIN_JAR_NAME:-sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar}"
sonarqube_home="${SONARQUBE_HOME:-/opt/sonarqube}"
plugin_source="${sonarqube_home}/lib/community-branch-plugin/${plugin_name}"
plugin_directory="${sonarqube_home}/extensions/plugins"
disabled_directory="${sonarqube_home}/extensions/disabled-plugins"
plugin_target="${plugin_directory}/${plugin_name}"

mkdir -p "${plugin_directory}" "${disabled_directory}"

plugin_temporary="${plugin_target}.tmp"

echo "Copying plugin from ${plugin_source} to temporary file ${plugin_temporary}"
cp "${plugin_source}" "${plugin_temporary}"

echo "Setting permissions (644) on ${plugin_temporary}"
chmod 644 "${plugin_temporary}"

for installed_plugin in "${plugin_directory}"/sonarqube-community-branch-plugin*.jar; do
  [ -f "${installed_plugin}" ] || continue
  [ "${installed_plugin}" = "${plugin_target}" ] && continue

  disabled_target="${disabled_directory}/$(basename "${installed_plugin}").disabled"
  echo "Disabling conflicting plugin: moving ${installed_plugin} to ${disabled_target}"
  mv "${installed_plugin}" "${disabled_target}"
done

echo "Installing plugin: moving ${plugin_temporary} to ${plugin_target}"
mv "${plugin_temporary}" "${plugin_target}"

echo "Executing SonarQube entrypoint: ${sonarqube_home}/docker/entrypoint.sh"
exec "${sonarqube_home}/docker/entrypoint.sh" "$@"
