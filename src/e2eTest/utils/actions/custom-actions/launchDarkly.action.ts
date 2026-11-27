import * as LaunchDarkly from '@launchdarkly/node-server-sdk';
import { Page, test } from '@playwright/test';

import { IAction, actionData, actionRecord } from '../../interfaces';

export const BAILIFF_MANAGEMENT_ENABLED = 'bailiff-management-enabled';

let ldClient: LaunchDarkly.LDClient | null = null;

export type LdFlagged<T> = { ldFlag: string; on: T; off: T };

export function whenLdFlag<T>(ldFlag: string, values: { on: T; off: T }): LdFlagged<T> {
  return { ldFlag, on: values.on, off: values.off };
}

export function isLdFlagged(value: unknown): value is LdFlagged<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as LdFlagged<unknown>).ldFlag === 'string' &&
    'on' in value &&
    'off' in value
  );
}

/** Throws when the value cannot be determined: unknown is not the same as off. */
export async function getLdFlag(flagKey: string): Promise<boolean> {
  const value = await new LaunchDarklyAction().getFlagValue(flagKey);
  if (value === undefined) {
    throw new Error(
      `Cannot evaluate LaunchDarkly flag "${flagKey}" (SDK key missing, initialisation failed or evaluation errored), so it is unknown which value to expect`
    );
  }
  return value;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const proto = Object.getPrototypeOf(value);
  return proto === Object.prototype || proto === null;
}

/**
 * Replaces every LdFlagged value found in plain objects and arrays with its on/off value.
 * Objects without flagged values are returned as the same reference.
 */
export async function resolveLdFlagged<T>(value: T): Promise<T> {
  if (isLdFlagged(value)) {
    const chosen = (await getLdFlag(value.ldFlag)) ? value.on : value.off;
    return (await resolveLdFlagged(chosen)) as T;
  }
  if (Array.isArray(value)) {
    const resolved = await Promise.all(value.map(item => resolveLdFlagged(item)));
    return (resolved.some((item, index) => item !== value[index]) ? resolved : value) as T;
  }
  if (isPlainObject(value)) {
    const entries = await Promise.all(
      Object.entries(value).map(async ([key, item]) => [key, await resolveLdFlagged(item)] as const)
    );
    const changed = entries.some(([key, item]) => item !== value[key]);
    return (changed ? Object.fromEntries(entries) : value) as T;
  }
  return value;
}

/**
 * Mirrors the app's LAUNCHDARKLY_ENV: `<env>` for pcs.<env>.platform.hmcts.net,
 * the full preview hostname for PR previews, otherwise the hostname.
 */
export function resolveLdEnvironment(): string | undefined {
  if (process.env.LAUNCHDARKLY_ENV) {
    return process.env.LAUNCHDARKLY_ENV;
  }
 
  const manageURL = process.env.MANAGE_CASE_BASE_URL
  if (!manageURL) {
    return undefined;
  }
  try {   
    const hostname = new URL(manageURL).hostname   
    const envMatch = /^xui-(pcs-api-pr-\d+)\.preview\.platform\.hmcts\.net$/.exec(hostname);
    return envMatch ? envMatch[1] : hostname;
  } catch {
    return undefined;
  }
}

export class LaunchDarklyAction implements IAction {
  async execute(page: Page, action: string, fieldName?: actionData | actionRecord): Promise<void> {
    const flagKey = fieldName as string;
    const flagValue = await this.getFlagValue(flagKey);
    test.skip(flagValue !== true, `Flag "${flagKey}" is OFF / could not be evaluated`);
  }

  /** Returns undefined when the real value cannot be determined. */
  async getFlagValue(flagKey: string): Promise<boolean | undefined> {
    try {
      if (!ldClient) {
        const sdkKey = process.env.LAUNCHDARKLY_SDK_KEY;
        if (!sdkKey) {
          console.error(`The SDK key is not set`);
          return undefined;
        }

        const client = LaunchDarkly.init(sdkKey);
        try {
          await client.waitForInitialization({ timeout: 10 });
        } catch (error) {
          client.close();
          throw error;
        }
        ldClient = client;
      }
      
      const context: LaunchDarkly.LDContext = { kind: 'user', key: resolveLdEnvironment() ?? 'default', environment: resolveLdEnvironment() };
      const detail = await ldClient.variationDetail(flagKey, context, false);
      if (detail.reason?.kind === 'ERROR') {
        console.error(`LaunchDarkly evaluation error for flag "${flagKey}": ${detail.reason.errorKind}`);
        return undefined;
      }
      return detail.value as boolean;
    } catch (error) {
      console.error(`LaunchDarkly error for flag "${flagKey}":`, error);
      return undefined;
    }
  }
}