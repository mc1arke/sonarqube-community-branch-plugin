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

import { Button, LinkStandalone, Tooltip, TooltipSide } from '@sonarsource/echoes-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { Image } from '~adapters/components/common/Image';
import { isDefined, isStringDefined } from '~shared/helpers/types';
import { PullRequest } from '~shared/types/branch-like';
import { AlmKeys } from '~sq-server-commons/types/alm-settings';
import { Component } from '~sq-server-commons/types/types';

function getPRUrlAlmKey(url = '') {
  const lowerCaseUrl = url.toLowerCase();

  if (lowerCaseUrl.includes(AlmKeys.GitHub)) {
    return AlmKeys.GitHub;
  } else if (lowerCaseUrl.includes(AlmKeys.GitLab)) {
    return AlmKeys.GitLab;
  } else if (lowerCaseUrl.includes(AlmKeys.BitbucketServer)) {
    return AlmKeys.BitbucketServer;
  } else if (
    lowerCaseUrl.includes(AlmKeys.Azure) ||
    lowerCaseUrl.includes('microsoft') ||
    lowerCaseUrl.includes('visualstudio')
  ) {
    return AlmKeys.Azure;
  }

  return undefined;
}

export function PRLinkLegacy({
  currentBranchLike,
  component,
}: Readonly<{
  component: Component;
  currentBranchLike: PullRequest;
}>) {
  const intl = useIntl();
  const almKey = component.alm?.key || getPRUrlAlmKey(currentBranchLike.url) || '';

  return (
    <>
      {isDefined(currentBranchLike.url) && (
        <LinkStandalone
          iconLeft={
            almKey !== '' && (
              <Image
                alt={almKey}
                height={16}
                src={`/images/alm/${almKey}.svg`}
                title={intl.formatMessage(
                  { id: 'branches.see_the_pr_on_x' },
                  { dop: intl.formatMessage({ id: `alm.${almKey}` }) },
                )}
              />
            )
          }
          key={currentBranchLike.key}
          to={currentBranchLike.url}
        >
          {almKey === '' && <FormattedMessage id="branches.see_the_pr" />}
        </LinkStandalone>
      )}
    </>
  );
}

interface Props {
  component: Component;
  pullRequest: PullRequest;
}

export default function PRLink({ pullRequest, component }: Readonly<Props>) {
  const almKey = component.alm?.key || getPRUrlAlmKey(pullRequest.url);

  if (!isDefined(pullRequest.url)) {
    return null;
  }

  return (
    <Tooltip
      content={
        <FormattedMessage
          id="project_navigation.binding_status.bound_to_x"
          values={{ dop: <FormattedMessage id={`alm.${almKey}`} /> }}
        />
      }
      side={TooltipSide.Top}
    >
      <Button
        prefix={
          isStringDefined(almKey) && (
            <Image alt={almKey} height={16} src={`/images/alm/${almKey}.svg`} width={16} />
          )
        }
        to={pullRequest.url}
      >
        {isStringDefined(almKey) ? (
          <FormattedMessage
            id="branches.see_the_pr_on_x"
            values={{ dop: <FormattedMessage id={`alm.${almKey}`} /> }}
          />
        ) : (
          <FormattedMessage id="branches.see_the_pr" />
        )}
      </Button>
    </Tooltip>
  );
}
