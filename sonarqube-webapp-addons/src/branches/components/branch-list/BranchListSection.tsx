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
import { useMemo } from 'react';
import { WithAvailableFeaturesProps } from '~sq-server-commons/context/available-features/withAvailableFeatures';
import { sortBranches } from '~sq-server-commons/helpers/branch-like';
import { translate } from '~sq-server-commons/helpers/l10n';
import { DEFAULT_NEW_CODE_DEFINITION_TYPE } from '~sq-server-commons/helpers/new-code-definition';
import { useNewCodeDefinitionQuery } from '~sq-server-commons/queries/newCodeDefinition';
import { isBranch } from '~sq-server-commons/sonar-aligned/helpers/branch-like';
import { AppState } from '~sq-server-commons/types/appstate';
import { Branch, BranchLike } from '~sq-server-commons/types/branch-like';
import { Component } from '~sq-server-commons/types/types';

import BranchList from './BranchList';

interface ProjectNewCodeDefinitionAppProps extends WithAvailableFeaturesProps {
  appState: AppState;
  branchLike: Branch;
  branchLikes: BranchLike[];
  component: Component;
}

export default function BranchListSection(props: Readonly<ProjectNewCodeDefinitionAppProps>) {
  const { component, branchLike, branchLikes } = props;

  const { data: globalNewCodeDefinition = { type: DEFAULT_NEW_CODE_DEFINITION_TYPE } } =
    useNewCodeDefinitionQuery();

  const { data: projectNewCodeDefinition } = useNewCodeDefinitionQuery({
    branchName: branchLike?.name,
    projectKey: component.key,
  });

  const branchList = useMemo(() => {
    return sortBranches(branchLikes.filter(isBranch));
  }, [branchLikes]);

  return (
    <div className="sw-mt-6">
      <Heading as="h3" className="sw-mb-4">
        {translate('project_baseline.configure_branches')}
      </Heading>

      <BranchList
        branchList={branchList}
        component={component}
        inheritedSetting={projectNewCodeDefinition ?? globalNewCodeDefinition}
        globalNewCodeDefinition={globalNewCodeDefinition}
      />
    </div>
  );
}
