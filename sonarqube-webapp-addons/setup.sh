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
export const baseViteConfig = { ...config, resolve: { ...config.resolve, preserveSymlinks: true } };
EOF
}

function patch_sq_server_vite_config() {
    local config_file="./apps/sq-server/vite.config.ts"
    
    # Check if already patched
    if grep -q "'~sq-server-commons':" "$config_file"; then
        echo "sq-server vite.config.ts already patched, skipping"
        return
    fi
    
    echo "Patching ${config_file} to add ~sq-server-commons alias"
    
    # Create backup if it doesn't exist
    if [[ ! -f "${config_file}.bak" ]]; then
        cp "$config_file" "${config_file}.bak"
    fi
    
    # Use a more robust approach with awk to add the alias after the closing of ~design-system
    awk '
    /~design-system.*resolve\(/ {
        in_design_system = 1
        print
        next
    }
    in_design_system && /\),/ {
        print
        print "        '\''~sq-server-commons'\'': resolve("
        print "          workspaceRoot,"
        print "          '\''libs/sq-server-commons/src'\'',"
        print "        ),"
        in_design_system = 0
        next
    }
    { print }
    ' "$config_file" > "${config_file}.new" && mv "${config_file}.new" "$config_file"
    
    echo "Successfully patched ${config_file}"
}

function patch_html_formatter_mock() {
    local mock_file="./libs/shared/src/components/typography/__mocks__/HtmlFormatter.tsx"
    
    # Check if already patched
    if grep -q "mockForwardRefComponent();" "$mock_file" 2>/dev/null; then
        echo "HtmlFormatter mock already patched, skipping"
        return
    fi
    
    echo "Patching ${mock_file} to remove sq-cloud dependency"
    
    # Create backup if it doesn't exist
    if [[ ! -f "${mock_file}.bak" ]]; then
        cp "$mock_file" "${mock_file}.bak"
    fi
    
    # Replace the import and mock with community-compatible version
    cat > "$mock_file" <<'EOF'
/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

// Community edition compatible mock - no sq-cloud dependency
const mockForwardRefComponent = (name: string) => {
  const Component = (props: any, ref: any) => null;
  Component.displayName = name;
  return Component;
};

export default mockForwardRefComponent('HtmlFormatter');
EOF
    
    echo "Successfully patched ${mock_file}"
}

function patch_administration_sidebar_test() {
    local test_file="./apps/sq-server/src/main/js/app/components/nav/administration/__tests__/AdministrationSidebar-test.tsx"
    
    # Check if already patched
    if grep -q "jest.mocked(addons).license = {} as any;" "$test_file" 2>/dev/null; then
        echo "AdministrationSidebar test already patched, skipping"
        return
    fi
    
    echo "Patching ${test_file} to fix license addon mock"
    
    # Create backup if it doesn't exist
    if [[ ! -f "${test_file}.bak" ]]; then
        cp "$test_file" "${test_file}.bak"
    fi
    
    # Replace line 33: jest.mocked(addons).license = undefined;
    # with: jest.mocked(addons).license = {} as any;
    sed -i.tmp 's/jest\.mocked(addons)\.license = undefined;/jest.mocked(addons).license = {} as any;/' "$test_file"
    rm -f "${test_file}.tmp"
    
    echo "Successfully patched ${test_file}"
}

function patch_mode_tour() {
    local file="./apps/sq-server/src/main/js/app/components/ModeTour.tsx"
    
    # Skip if file doesn't exist (removed in later commits)
    if [ ! -f "$file" ]; then
        echo "ModeTour.tsx not found (removed in newer versions), skipping patch"
        return
    fi
    
    # Check if already patched
    if grep -q "// Disabled for Community Edition" "$file" 2>/dev/null; then
        echo "ModeTour already patched, skipping"
        return
    fi
    
    echo "Patching ModeTour.tsx..."
    
    # Create backup if it doesn't exist
    if [[ ! -f "${file}.bak" ]]; then
        cp "$file" "${file}.bak"
    fi
    
    # Insert return null after function declaration
    # Use perl for cross-platform compatibility
    perl -i -pe 's/(^export default function ModeTour\(\) \{$)/$1\n  \/\/ Disabled for Community Edition: server does not recognize showNewModesTour notice\n  return null;/' "$file"
    
    echo "ModeTour patched successfully"
}

function patch_license_api() {
    local file="./libs/sq-server-commons/src/api/entitlements.ts"
    
    if [ ! -f "$file" ]; then
        echo "entitlements.ts not found, skipping patch"
        return
    fi
    
    if grep -q "// Disabled for Community Edition" "$file" 2>/dev/null; then
        echo "License API already patched, skipping"
        return
    fi
    
    echo "Patching entitlements.ts..."
    # Match function with or without async, with any return type annotation
    perl -i.bak -pe 's/(^export (?:async )?function getCurrentLicense\([^)]*\)(?::[^{]*)?\{$)/$1\n  \/\/ Disabled for Community Edition: endpoint does not exist\n  return null;/' "$file" && rm -f "$file.bak"
    echo "License API patched successfully"
}

function main() {
    cd ./sonarqube-webapp
    echo "Creating symlink for sonarqube-webapp/libs/sq-server-addons"
    rm -rf ./libs/sq-server-addons
    ln -s "${CURRENT_DIR}/sonarqube-webapp-addons" ./libs/sq-server-addons
    override_vite_config
    patch_sq_server_vite_config
    patch_html_formatter_mock
    patch_administration_sidebar_test
    patch_mode_tour
    patch_license_api
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
