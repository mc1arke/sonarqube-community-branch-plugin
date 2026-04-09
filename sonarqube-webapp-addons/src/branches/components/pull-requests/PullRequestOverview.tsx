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

import { Layout, Spinner } from '@sonarsource/echoes-react';
import { uniq } from 'lodash';
import { useIntl } from 'react-intl';
import { useFlags } from '~adapters/helpers/feature-flags';
import { BasicSeparator } from '~design-system';
import { ProjectPageTemplate } from '~shared/components/pages/ProjectPageTemplate';
import { isDefined } from '~shared/helpers/types';
import { PullRequest } from '~shared/types/branch-like';
import { ComponentNavBindingStatus } from '~sq-server-commons/components/nav/ComponentNavBindingStatus';
import { AnalysisStatus } from '~sq-server-commons/components/overview/AnalysisStatus';
import IgnoredConditionWarning from '~sq-server-commons/components/overview/IgnoredConditionWarning';
import LastAnalysisLabel from '~sq-server-commons/components/overview/LastAnalysisLabel';
import QGStatus from '~sq-server-commons/components/overview/QualityGateStatus';
import '~sq-server-commons/components/overview/styles.css';
import './pr-overview-fix.css';
import { useAvailableFeatures } from '~sq-server-commons/context/available-features/withAvailableFeatures';
import {
  enhanceConditionWithMeasure,
  enhanceMeasuresWithMetrics,
} from '~sq-server-commons/helpers/measures';
import { useBranchStatusQuery } from '~sq-server-commons/queries/branch';
import { useMeasuresComponentQuery } from '~adapters/queries/measures';
import { useComponentQualityGateQuery } from '~sq-server-commons/queries/quality-gates';
import { Feature } from '~sq-server-commons/types/features';
import { Component } from '~sq-server-commons/types/types';
import { PR_METRICS } from '~sq-server-commons/utils/overview-utils';
import PRLink from '../branch-like/PRLink';
import MeasuresCardPanel from './MeasuresCardPanel';
import { PullRequestMetaContentHeader } from './PullRequestMetaContentHeader';
import PullRequestMetaTopBar from './PullRequestMetaTopBar';
import SonarLintAd from './SonarLintAd';

interface Props {
  component: Component;
  pullRequest: PullRequest;
}

export default function PullRequestOverview(props: Readonly<Readonly<Props>>) {
  const { component, pullRequest } = props;
  const intl = useIntl();
  const {
    data: { conditions, ignoredConditions, status } = {},
    isLoading: isLoadingBranchStatusesData,
  } = useBranchStatusQuery(component);

  const { data: qualityGate, isLoading: isLoadingQualityGate } = useComponentQualityGateQuery(
    component.key,
  );

  const { frontEndEngineeringEnableSidebarNavigation } = useFlags();
  const { hasFeature } = useAvailableFeatures();
  const hasBranchSupport = hasFeature(Feature.BranchSupport);
  const pageTitle = intl.formatMessage({ id: 'overview.page' });

  const { data: componentMeasures, isLoading: isLoadingMeasures } = useMeasuresComponentQuery(
    {
      componentKey: component.key,
      metricKeys: uniq([...PR_METRICS, ...(conditions?.map((c) => c.metric) ?? [])]),
      branchLike: pullRequest,
    },
    { enabled: !isLoadingBranchStatusesData },
  );

  const measures = componentMeasures
    ? enhanceMeasuresWithMetrics(
        componentMeasures.component.measures ?? [],
        componentMeasures.metrics,
      )
    : [];

  const isLoading = isLoadingBranchStatusesData || isLoadingMeasures || isLoadingQualityGate;

  if (isLoading) {
    return (
        <ProjectPageTemplate disableQualityGateStatus title={pageTitle}>
          <Spinner isLoading />
        </ProjectPageTemplate>
    );
  }

  if (conditions === undefined) {
    return null;
  }

  const enhancedConditions = conditions
    .map((c) => enhanceConditionWithMeasure(c, measures))
    .filter(isDefined);

  return (
      <ProjectPageTemplate
        actions={
            <Layout.ContentHeader.Actions>
              <ComponentNavBindingStatus
                component={component}
                prLinkAddon={
                  hasBranchSupport && <PRLink component={component} pullRequest={pullRequest} />
                }
              />
            </Layout.ContentHeader.Actions>
          }
        disableQualityGateStatus
        metadata={<PullRequestMetaContentHeader measures={measures} pullRequest={pullRequest} />}
        pageClassName="it__pr-overview"
        title={pageTitle}
      >
        {!frontEndEngineeringEnableSidebarNavigation && (
          <>
            <PullRequestMetaTopBar measures={measures} pullRequest={pullRequest} />
            <BasicSeparator className="sw-my-4" />
          </>
        )}

        {ignoredConditions && <IgnoredConditionWarning />}

        <div className="sw-flex sw-justify-between sw-items-start sw-my-6">
          <QGStatus status={status} />
          {!frontEndEngineeringEnableSidebarNavigation && (
            <LastAnalysisLabel analysisDate={pullRequest.analysisDate} />
          )}
        </div>

        <AnalysisStatus className="sw-mb-4" component={component} />

        <MeasuresCardPanel
          pullRequest={pullRequest}
          component={component}
          conditions={enhancedConditions}
          qualityGate={qualityGate}
          measures={measures}
        />

        <SonarLintAd status={status} />
      </ProjectPageTemplate>
  );
}
