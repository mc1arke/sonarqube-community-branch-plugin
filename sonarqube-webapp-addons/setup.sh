#!/usr/bin/env bash

set -Eeuo pipefail

CURRENT_DIR=$(pwd)

function override_vite_config() {
    if [[ -f ./vite.config.src.ts ]]; then
        echo "vite.config.src.ts exists, skipping override"
        return
    fi
    echo "Overriding config vite.config.base.ts"
    mv ./vite.config.base.ts ./vite.config.src.ts
    cat <<EOF >vite.config.base.ts
import { baseViteConfig as config } from './vite.config.src';

export * from './vite.config.src';
export const baseViteConfig = { ...config, resolve: { preserveSymlinks: true } };
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
