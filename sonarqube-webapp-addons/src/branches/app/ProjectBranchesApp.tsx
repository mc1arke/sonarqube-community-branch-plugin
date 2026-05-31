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

import * as React from 'react';
import { useIntl } from 'react-intl';
import { ProjectPageTemplate } from '~shared/components/pages/ProjectPageTemplate';
import withComponentContext from '~sq-server-commons/context/componentContext/withComponentContext';
import { withBranchLikes } from '~sq-server-commons/queries/branch';
import { Component } from '~sq-server-commons/types/types';
import BranchLikeTabs from './components/BranchLikeTabs';
import LifetimeInformation from './components/LifetimeInformation';

export interface ProjectBranchesAppProps {
  component: Component;
  fetchComponent: () => Promise<void>;
}

function ProjectBranchesApp(props: ProjectBranchesAppProps) {
  const { component, fetchComponent } = props;
  const intl = useIntl();

  const title = intl.formatMessage({ id: 'project_branch_pull_request.page' });

  return (
    <ProjectPageTemplate description={<LifetimeInformation />} disableBranchSelector title={title}>
      <div id="project-branch-like">
        <BranchLikeTabs component={component} fetchComponent={fetchComponent} />
      </div>
    </ProjectPageTemplate>
  );
}

export default withComponentContext(withBranchLikes(React.memo(ProjectBranchesApp)));
