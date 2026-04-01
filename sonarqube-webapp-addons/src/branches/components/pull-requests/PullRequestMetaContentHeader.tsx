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

import { FormattedMessage } from 'react-intl';
import { SeparatorCircleIcon } from '~design-system';
import { PullRequest } from '~shared/types/branch-like';
import { MeasureEnhanced } from '~shared/types/measures';
import { MetricKey, MetricType } from '~shared/types/metrics';
import { getLeakValue } from '~sq-server-commons/components/measure/utils';
import LastAnalysisLabel from '~sq-server-commons/components/overview/LastAnalysisLabel';
import { findMeasure } from '~sq-server-commons/helpers/measures';
import { formatMeasure } from '~sq-server-commons/sonar-aligned/helpers/measures';
import { CurrentBranchLikeMergeInformation } from '../branch-like/CurrentBranchLikeMergeInformation';

interface Props {
  measures: MeasureEnhanced[];
  pullRequest: PullRequest;
}

export function PullRequestMetaContentHeader({ measures, pullRequest }: Readonly<Props>) {
  return (
    <>
      {pullRequest?.analysisDate && (
        <>
          <LastAnalysisLabel analysisDate={pullRequest.analysisDate} />
          <SeparatorCircleIcon />
        </>
      )}
      <span>
    {formatMeasure(
      getLeakValue(findMeasure(measures, MetricKey.new_lines)),
      MetricType.ShortInteger,
    ) || '0'}{' '}
        <FormattedMessage id="metric.new_lines.name" />
    </span>
      <SeparatorCircleIcon />
      <CurrentBranchLikeMergeInformation pullRequest={pullRequest} />
    </>
  );
}
