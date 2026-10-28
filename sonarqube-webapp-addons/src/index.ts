import branches from './branches';
import * as entitlements from './feature-license/entitlements';

const globalAddons = {
  branches,
  entitlements,
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type AddonsType = { [key: string]: any | undefined } & typeof globalAddons;
export const addons: AddonsType = { ...globalAddons };
