/*
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

import { ComponentBase } from '~shared/types/component';

export enum AlmType {
    Azure = 'azure',
    Bitbucket = 'bitbucket',
    GitHub = 'github',
    GitLab = 'gitlab',
}

type AlmIntegrationDocLinkKey = AlmType.Bitbucket | AlmType.GitHub | AlmType.GitLab | 'microsoft';

export function getComponentAlmKey(component: ComponentBase): AlmIntegrationDocLinkKey | undefined {
    return component.alm && sanitizeAlmId(component.alm.key);
}

export function sanitizeAlmId(almKey: string): AlmIntegrationDocLinkKey {
    if (isBitbucket(almKey)) {
        return AlmType.Bitbucket;
    }

    if (isAzure(almKey)) {
        return 'microsoft';
    }

    return almKey as AlmIntegrationDocLinkKey;
}

export function isBitbucket(almKey?: string): boolean {
    return Boolean(almKey?.startsWith('bitbucket'));
}

export function isAzure(almKey?: string): boolean {
    return Boolean(almKey === 'microsoft' || almKey?.startsWith('azure'));
}
