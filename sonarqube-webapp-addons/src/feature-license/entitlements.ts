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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

import { queryOptions, useQueries } from '@tanstack/react-query';
import { createQueryHook, StaleTime } from '~shared/queries/common';
import {
  combineEntitlementChecks,
  EntitlementCheckParams,
  notEntitled,
} from '~shared/queries/entitlement-checks';
import { EntitlementCheckFeatureKey } from '~shared/types/billing';
import { PurchaseableFeature } from '~sq-server-commons/types/editions';

export const useEntitlementCheckQuery = createQueryHook((params: EntitlementCheckParams) =>
  queryOptions({
    queryKey: ['entitlement-check', params.featureKey, params.resourceType, params.resourceId],
    queryFn: () => Promise.resolve(notEntitled(params.featureKey)),
    staleTime: StaleTime.LIVE,
  }),
);

export function useEntitlementChecksQuery(featureKeys: readonly EntitlementCheckFeatureKey[]) {
  const usableKeys = featureKeys.filter((key) => key.length > 0);

  return useQueries({
    queries: usableKeys.map((featureKey) =>
      queryOptions({
        queryKey: ['entitlement-check', featureKey],
        queryFn: () => Promise.resolve(notEntitled(featureKey)),
        staleTime: StaleTime.LIVE,
      }),
    ),
    combine: combineEntitlementChecks,
  });
}

export const usePurchasableFeature = createQueryHook((featureKey: string) =>
  queryOptions({
    queryKey: ['purchasable-feature', featureKey],
    queryFn: (): Promise<PurchaseableFeature[]> => Promise.resolve([]),
    select: (features): PurchaseableFeature | undefined =>
      features.find((feature) => feature.featureKey === featureKey),
    staleTime: StaleTime.NEVER,
  }),
);
