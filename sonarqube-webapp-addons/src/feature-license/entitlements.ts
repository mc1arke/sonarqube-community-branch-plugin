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

import { queryOptions, useQueries } from '@tanstack/react-query';
import { createQueryHook, StaleTime } from '~shared/queries/common';
import {
  combineEntitlementChecks,
  EntitlementCheckParams,
  notEntitled,
} from '~shared/queries/entitlement-checks';
import { EntitlementCheckFeatureKey } from '~shared/types/billing';
import { PurchasableFeatureKey, PurchaseableFeature } from '~sq-server-commons/types/editions';

/**
 * Community edition stub. There is no billing backend, so every feature
 * resolves as not entitled. Commercial builds never load this file — Vite
 * and tsconfig resolve the private addons barrel instead.
 */
export const useEntitlementCheckQuery = createQueryHook((params: EntitlementCheckParams) =>
  queryOptions({
    queryKey: ['entitlement-check', params.featureKey, params.resourceType, params.resourceId],
    queryFn: () => Promise.resolve(notEntitled(params.featureKey)),
    staleTime: StaleTime.LIVE,
  }),
);

/** Fan-out stub mirroring the commercial API shape for community builds. */
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

/**
 * Community edition stub. The `purchasable-features` endpoint returns nothing on CE, so no
 * feature is ever purchasable. Shape mirrors the commercial hook's `useQuery` result.
 */
export const usePurchasableFeature = createQueryHook((featureKey: PurchasableFeatureKey) =>
  queryOptions({
    queryKey: ['purchasable-feature', featureKey],
    queryFn: (): Promise<PurchaseableFeature[]> => Promise.resolve([]),
    select: (features): PurchaseableFeature | undefined =>
      features.find((f) => f.featureKey === featureKey),
    staleTime: StaleTime.NEVER,
  }),
);
