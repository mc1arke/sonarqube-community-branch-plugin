/*
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

import styled from '@emotion/styled';
import { cssVar, IconCheck, Text, Tooltip } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import {
    createPath,
    createSearchParams,
    Link,
    useResolvedPath,
    useSearchParams,
} from 'react-router-dom';
import { QualityGateIndicator } from '~adapters/components/ui/QualityGateIndicator';
import { BranchLikeIcon } from '~shared/components/icon-mappers/BranchLikeIcon';
import {
    getBranchLikeDisplayName,
    isBranch,
    isMainBranch,
    isPullRequest,
} from '~shared/helpers/branch-like';
import { BranchLikeBase, ProjectBranchSelectorProps } from '~shared/types/branch-like';

interface Props {
    branchLike: BranchLikeBase;
    isChecked: boolean;
    overridePath?: ProjectBranchSelectorProps['overridePath'];
}

export function ProjectBranchSelectorItem(props: Readonly<Props>) {
    const { branchLike, isChecked, overridePath } = props;

    const targetPath = useMergedPath(branchLike, overridePath);

    return (
        <Tooltip content={getBranchLikeDisplayName(branchLike)} side="right">
            <StyledBranchLikeLink className="sw-pl-1 -sw-ml-1 sw-pr-2 -sw-mr-2" to={targetPath}>
                <div className="sw-flex sw-items-center sw-py-2 sw-pr-3 sw-gap-2 sw-min-w-0">
                    <Text size="default">
                        <IconCheck
                            className={classNames({ 'sw-invisible': !isChecked })}
                            color="echoes-color-icon-selected"
                        />
                    </Text>

                    <BranchLikeIcon
                        branchLike={branchLike}
                        className="sw-shrink-0"
                        color={isChecked ? 'echoes-color-icon-default' : 'echoes-color-icon-subtle'}
                    />

                    <Text className="sw-truncate" isHighlighted={isChecked}>
                        {getBranchLikeDisplayName(branchLike)}
                    </Text>
                </div>

                <div className="sw-flex sw-items-center sw-py-2">
                    <QualityGateIndicator
                        className="sw-mr-2"
                        size="sm"
                        status={branchLike.status?.qualityGateStatus ?? 'NONE'}
                    />
                    <div className="sw-min-w-800">
                        <FormattedMessage
                            id={`metric.level.${branchLike.status?.qualityGateStatus ?? 'NONE'}`}
                        />
                    </div>
                </div>
            </StyledBranchLikeLink>
        </Tooltip>
    );
}

export function useMergedPath(
    branchLike: BranchLikeBase,
    overridePath?: ProjectBranchSelectorProps['overridePath'],
) {
    // Resolve the override path - can be a static To or a function
    const resolvedOverridePath =
        typeof overridePath === 'function' ? overridePath(branchLike) : overridePath;

    // Start path is either the current or the overridden path
    const startPath = useResolvedPath(resolvedOverridePath || {});
    const [startSearchParams] = useSearchParams();
    const searchParams = createSearchParams(
        resolvedOverridePath?.search?.toString() ?? startSearchParams,
    );

    // Remove current branch-like parameters
    searchParams.delete('branch');
    searchParams.delete('pullRequest');

    if (isBranch(branchLike) && !isMainBranch(branchLike)) {
        searchParams.append('branch', branchLike.name);
    } else if (isPullRequest(branchLike)) {
        searchParams.append('pullRequest', branchLike.key);
    }

    return createPath({ ...startPath, search: searchParams.toString() });
}

const StyledBranchLikeLink = styled(Link)`
  all: unset;

  box-sizing: border-box;
  width: 100%;

  background-color: ${cssVar('color-surface-default')};
  border-radius: ${cssVar('border-radius-200')};
  color: ${cssVar('color-text-default')};
  font: ${cssVar('typography-text-default-regular')};

  display: flex;
  align-items: center;
  justify-content: space-between;
  column-gap: ${cssVar('dimension-space-100')};

  cursor: pointer;

  &:focus-visible {
    border-radius: ${cssVar('border-radius-400')};
    outline: ${cssVar('color-focus-default')} solid ${cssVar('focus-border-width-default')};
    outline-offset: -2px;
  }

  &:hover {
    background-color: ${cssVar('color-surface-hover')};
    outline: none;
  }

  /* when the item is clicked */
  &:active {
    background-color: ${cssVar('color-surface-active')};
  }

  &[data-disabled] {
    background-color: ${cssVar('color-surface-default')};
    color: ${cssVar('color-text-disabled')};
    cursor: default;
  }
`;
