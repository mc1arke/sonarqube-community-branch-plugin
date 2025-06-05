/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import { Heading } from '@sonarsource/echoes-react';
import { translate } from '~sq-server-commons/helpers/l10n';
import type { Branch } from '~sq-server-commons/types/branch-like';
import type { NewCodeDefinition } from '~sq-server-commons/types/new-code-definition';
import type { Component } from '~sq-server-commons/types/types';
import BranchList from './BranchList';

interface Props {
  branchList: Branch[];
  component: Component;
  globalNewCodeDefinition: NewCodeDefinition;
  projectNewCodeDefinition: NewCodeDefinition;
}

export default function BranchListSection(props: Readonly<Props>) {
  const { branchList, component, globalNewCodeDefinition, projectNewCodeDefinition } = props;

  return (
    <div className="sw-mt-6">
      <Heading as="h3" className="sw-mb-4">
        {translate('project_baseline.configure_branches')}
      </Heading>

      <BranchList
        branchList={branchList}
        component={component}
        inheritedSetting={projectNewCodeDefinition}
        globalNewCodeDefinition={globalNewCodeDefinition}
      />
    </div>
  );
}
