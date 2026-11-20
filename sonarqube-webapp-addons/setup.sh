#!/usr/bin/env bash

set -Eeuo pipefail

CURRENT_DIR=$(pwd)

function override_vite_config() {
    if [[ ! -f ./vite.config.src.ts ]]; then
        echo "Moving vite.config.base.ts to vite.config.src.ts"
        mv ./vite.config.base.ts ./vite.config.src.ts
    fi
    echo "Overriding config vite.config.base.ts"
    cat <<EOF >vite.config.base.ts
import { resolve } from 'node:path';
import { baseViteConfig as config } from './vite.config.src';

export * from './vite.config.src';
export const baseViteConfig = {
  ...config,
  resolve: {
    ...config.resolve,
    preserveSymlinks: true,
    alias: {
      ...config.resolve?.alias,
      '~sq-server-commons': resolve(__dirname, 'libs/sq-server-commons/src'),
    },
  },
};
EOF
}

function main() {
    cd ./sonarqube-webapp
    echo "Creating symlink for sonarqube-webapp/libs/sq-server-addons"
    rm -rf ./libs/sq-server-addons
    ln -s "${CURRENT_DIR}/sonarqube-webapp-addons" ./libs/sq-server-addons
    override_vite_config
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
