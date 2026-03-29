/*
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

import styled from '@emotion/styled';
import {
    Button,
    IconChevronDown,
    LinkStandalone,
    Popover,
    SearchInput,
    ToggleButtonGroup,
} from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { compareDesc } from 'date-fns';
import { useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { isSameBranchLike } from '~adapters/helpers/branch-like';
import { useProjectBranchesQuery } from '~adapters/queries/branch';
import { BranchLikeIcon } from '~shared/components/icon-mappers/BranchLikeIcon';
import {
    getBranchLikeDisplayName,
    isBranch,
    isMainBranch,
    isPullRequest,
} from '~shared/helpers/branch-like';
import { isDefined, isStringDefined } from '~shared/helpers/types';
import { BranchLikeBase, ProjectBranchSelectorProps } from '~shared/types/branch-like';
import { getComponentAlmKey } from '../../helpers/alm-integrations';
import { PRMessage } from '../common/PRMessage';
import { ProjectBranchSelectorItem } from './ProjectBranchSelectorItem';

const MAX_BRANCHES_OR_PRS_TO_SHOW = 10;

enum BranchLikeFilter {
    'All' = 'all',
    'Branches' = 'branches',
    'PullRequests' = 'pull_requests',
}

export default function ProjectBranchSelector(props: Readonly<ProjectBranchSelectorProps>) {
    const {
        className,
        component,
        currentBranchLike,
        linkToAll,
        linkToBranches,
        linkToPRs,
        overridePath,
    } = props;

    const { formatMessage } = useIntl();

    const [query, setQuery] = useState('');
    const [filter, setFilter] = useState(BranchLikeFilter.All);

    const { data: branchLikes } = useProjectBranchesQuery(component);

    const branchLikesToDisplay = getBranchLikesToDisplay(filter, query, branchLikes);

    const getOptions = (prLabel: string) => [
        {
            label: formatMessage({ id: BranchLikeFilter.All }),
            value: BranchLikeFilter.All,
        },
        {
            label: formatMessage({ id: BranchLikeFilter.Branches }),
            value: BranchLikeFilter.Branches,
        },
        {
            label: prLabel,
            value: BranchLikeFilter.PullRequests,
        },
    ];

    return (
        <StyledPopover
            extraContent={
                <div className="sw-mt-4">
                    <SearchInput
                        ariaLabel={formatMessage({ id: 'project_branch_selector.search' })}
                        className="sw-mb-4 sw-max-w-5000"
                        onChange={setQuery}
                        placeholderLabel={formatMessage({ id: 'project_branch_selector.search' })}
                        value={query}
                    />

                    <div className="sw-flex sw-justify-between sw-items-center sw-mb-3">
                        <PRMessage almKey={getComponentAlmKey(component)} message="{pull_requests}">
                            {(prLabel) => (
                                <>
                                    <ToggleButtonGroup
                                        onChange={(v: BranchLikeFilter) => {
                                            setFilter(v);
                                        }}
                                        options={getOptions(prLabel)}
                                        selected={filter}
                                    />
                                    {linkToAll && filter === BranchLikeFilter.All && (
                                        <LinkStandalone to={linkToAll}>
                                            <FormattedMessage id="project_branch_selector.link.all" />
                                        </LinkStandalone>
                                    )}
                                    {linkToBranches && filter === BranchLikeFilter.Branches && (
                                        <LinkStandalone to={linkToBranches}>
                                            <FormattedMessage id="project_branch_selector.link.branches" />
                                        </LinkStandalone>
                                    )}
                                    {linkToPRs && filter === BranchLikeFilter.PullRequests && (
                                        <LinkStandalone to={linkToPRs}>
                                            <FormattedMessage
                                                id="project_branch_selector.link.pull_requests"
                                                values={{ pull_requests: prLabel.toLocaleLowerCase() }}
                                            />
                                        </LinkStandalone>
                                    )}
                                </>
                            )}
                        </PRMessage>
                    </div>

                    {branchLikesToDisplay.length > 0 ? (
                        <ul className="sw-w-full">
                            {branchLikesToDisplay.map((branchLike) => (
                                <li key={getBranchLikeDisplayName(branchLike)}>
                                    <ProjectBranchSelectorItem
                                        branchLike={branchLike}
                                        isChecked={isSameBranchLike(currentBranchLike, branchLike)}
                                        overridePath={overridePath}
                                    />
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <div className="sw-flex sw-items-center sw-justify-center sw-my-4">
                            <FormattedMessage id="project_branch_selector.none" />
                        </div>
                    )}
                </div>
            }
            title={<FormattedMessage id="project_branch_selector.title" />}
        >
            <Button
                className={classNames(className, 'sw-max-w-5000')}
                prefix={<BranchLikeIcon branchLike={currentBranchLike} />}
                size="medium"
                suffix={<IconChevronDown />}
            >
                {getBranchLikeDisplayName(currentBranchLike)}
            </Button>
        </StyledPopover>
    );
}

const StyledPopover = styled(Popover)`
  max-width: unset;
  width: 600px;
`;

function getBranchLikesToDisplay(
    filter: BranchLikeFilter,
    query: string,
    branchLikes: BranchLikeBase[] = [],
) {
    const organizedBranchLikes = branchLikes
        .filter((b) => {
            // remove non-analyzed branches and the main branch
            if (!isStringDefined(b.analysisDate) || isMainBranch(b)) {
                return false;
            }

            if (!matchSearch(query, b)) {
                return false;
            }

            if (filter === BranchLikeFilter.All) {
                return true;
            }

            return (
                (isPullRequest(b) && filter === BranchLikeFilter.PullRequests) ||
                (isBranch(b) && filter === BranchLikeFilter.Branches)
            );
        })
        .sort((a, b) => compareDesc(a.analysisDate!, b.analysisDate!))
        .slice(0, MAX_BRANCHES_OR_PRS_TO_SHOW);

    // Add the main branch at the top
    if ([BranchLikeFilter.All, BranchLikeFilter.Branches].includes(filter)) {
        const mainBranch = branchLikes.find(isMainBranch);

        if (matchSearch(query, mainBranch)) {
            return [mainBranch, ...organizedBranchLikes].filter(isDefined);
        }
    }

    return organizedBranchLikes;
}

function matchSearch(query: string, branchLike?: BranchLikeBase) {
    if (!isDefined(branchLike)) {
        return false;
    }

    const label = getBranchLikeDisplayName(branchLike);
    const q = query.toLowerCase();

    return (
        // No search means everything matches
        query.length === 0 ||
        // Check the label
        label.toLowerCase().includes(q) ||
        // Check the branch name for PRs
        (isPullRequest(branchLike) && branchLike.branch.toLowerCase().includes(q))
    );
}
