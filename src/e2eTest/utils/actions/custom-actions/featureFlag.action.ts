import Axios from 'axios';
import { Page, test } from '@playwright/test';
import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { createCaseEventTokenApiData, getCaseApiData } from '@data/api-data';

/**
 * The flags pcs-api returns on every case under featureFlags (FeatureFlags.java, filled by
 * FeatureFlagView from LaunchDarkly with pcs-api's own context). The definition's show conditions
 * read these values, so they are what the UI sees. A LaunchDarkly flag not listed here is not on
 * the case: add it to FeatureFlags.java and FeatureFlagView first.
 */
export const FeatureFlags = {
  RELEASE_1_DOT_2: 'release1dot2Enabled',
  RELEASE_1_DOT_3: 'release1dot3Enabled',
  RELEASE_1B: 'release1bEnabled',
  RELEASE_1_DOT_4: 'release1dot4Enabled',
  CASEWORKER_EVENTS: 'caseWorkerEventsEnabled',
  WALES_MAKE_A_CLAIM: 'walesMakeAClaimEnabled',
  CUI_RESPOND_TO_CLAIM_LR: 'cuiRespondToClaimLrEnabled',
  MAKE_ORDER: 'makeOrderEnabled',
  ENFORCEMENT: 'enforcementEnabled',
  CASE_MANAGEMENT_R2: 'caseManagementR2Enabled',
  BAILIFF_MANAGEMENT_ENABLED: 'bailiffManagementEnabled',
} as const;

export type FeatureFlagName = (typeof FeatureFlags)[keyof typeof FeatureFlags];

// Flags don't change during a test, so each case's featureFlags is fetched once.
const featureFlagsByCase = new Map<string, Record<string, unknown>>();

/**
 * Whether a flag is on for the current case (CASE_NUMBER), read with the token createCaseAPI set.
 * Throws when the value can't be read: unknown is not the same as off.
 */
export async function isFeatureFlagEnabled(flag: FeatureFlagName): Promise<boolean> {
  const caseNumber = process.env.CASE_NUMBER ?? '';
  if (!featureFlagsByCase.has(caseNumber)) {
    featureFlagsByCase.set(caseNumber, await fetchFeatureFlags());
  }
  const value = featureFlagsByCase.get(caseNumber)?.[flag];
  if (value !== 'YES' && value !== 'NO') {
    throw new Error(`Feature flag "${flag}" not returned on case ${caseNumber} (got ${JSON.stringify(value)})`);
  }
  console.log(`\n✅ Feature flag "${flag}" is ${value === 'YES' ? 'ON' : 'OFF'}`);
  return value === 'YES';
}

async function fetchFeatureFlags(): Promise<Record<string, unknown>> {
  const api = Axios.create(createCaseEventTokenApiData.createCaseEventTokenApiInstance());
  const response = await api.get(getCaseApiData.getCaseApiEndPoint());
  return response.data?.data?.featureFlags ?? {};
}

/** skipTestIfFeatureFlagDisabled / skipTestIfFeatureFlagEnabled, with a FeatureFlags value. */
export class FeatureFlagAction implements IAction {
  async execute(page: Page, action: string, fieldName?: actionData | actionRecord): Promise<void> {
    const flag = fieldName as FeatureFlagName;
    if (action === 'skipTestIfFeatureFlagDisabled') {
      test.skip(!(await isFeatureFlagEnabled(flag)), `Feature flag "${flag}" is OFF`);
    } else if (action === 'skipTestIfFeatureFlagEnabled') {
      test.skip(await isFeatureFlagEnabled(flag), `Feature flag "${flag}" is ON`);
    } else {
      throw new Error(`No action found for '${action}'`);
    }
  }
}
