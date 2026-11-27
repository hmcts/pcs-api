import Axios from 'axios';
import { Page, test } from '@playwright/test';
import { actionData, actionRecord, IAction } from '@utils/interfaces';

/** LaunchDarkly keys of pcs-api's FeatureFlag enum. */
export const FeatureFlags = {
  CASE_MANAGEMENT_R2: 'case-management-r-2-enabled',
  BAILIFF_MANAGEMENT_ENABLED: 'bailiff-management-enabled',
} as const;

export type FeatureFlagName = (typeof FeatureFlags)[keyof typeof FeatureFlags];

// Flags don't change during a run, so each is asked for once.
const flagValues = new Map<string, boolean>();

/**
 * Whether a flag is on in the pcs-api under test, from its testing-support endpoint (pcs-api's own
 * LaunchDarkly context). Throws when the value can't be read: unknown is not the same as off.
 */
export async function isFeatureFlagEnabled(flag: FeatureFlagName): Promise<boolean> {
  if (!flagValues.has(flag)) {
    const response = await Axios.get(`${process.env.CASE_API_URL}/testing-support/feature-flags/${flag}`, {
      headers: { ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}` },
    });
    if (typeof response.data !== 'boolean') {
      throw new Error(`Feature flag "${flag}" not returned by pcs-api (got ${JSON.stringify(response.data)})`);
    }
    flagValues.set(flag, response.data);
  }
  const enabled = flagValues.get(flag) as boolean;
  console.log(`\n✅ Feature flag "${flag}" is ${enabled ? 'ON' : 'OFF'}`);
  return enabled;
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
