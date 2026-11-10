#!/bin/sh

set -eu

plugin_name="sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar"
plugin_source="/opt/sonarqube/lib/community-branch-plugin/${plugin_name}"
plugin_directory="/opt/sonarqube/extensions/plugins"
disabled_directory="/opt/sonarqube/extensions/disabled-plugins"
plugin_target="${plugin_directory}/${plugin_name}"

mkdir -p "${plugin_directory}" "${disabled_directory}"

for installed_plugin in "${plugin_directory}"/sonarqube-community-branch-plugin-*.jar; do
  [ -f "${installed_plugin}" ] || continue
  [ "${installed_plugin}" = "${plugin_target}" ] ||
    mv "${installed_plugin}" "${disabled_directory}/$(basename "${installed_plugin}").disabled"
done

plugin_temporary="${plugin_target}.tmp"
cp "${plugin_source}" "${plugin_temporary}"
chmod 644 "${plugin_temporary}"
mv "${plugin_temporary}" "${plugin_target}"

exec /opt/sonarqube/docker/entrypoint.sh "$@"
