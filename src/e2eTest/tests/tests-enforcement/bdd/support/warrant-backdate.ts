/**
 * Moving a warrant's generation date into the past, so the 12 calendar month free repeat warrant period
 * (HDPI-8424) can be tested without waiting a year. The free period runs from when the claim's first warrant
 * was generated, which in these scenarios is the warrant the scenario has just closed.
 */
import { expect } from '@playwright/test';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { backdateEnforcement } from './backdate-enforcement';
import { requireCaseReference } from './enforcement-journeys';

/** Backdates the warrant this scenario last closed by `months` calendar months. */
export async function backdateWarrantGeneration(world: EnforcementWorld, months: number): Promise<void> {
  const enforcementNumber = world.earlierWarrantNumbers.at(-1);
  expect(enforcementNumber, 'a closed warrant on the case to backdate').toBeDefined();
  await backdateEnforcement(requireCaseReference(world), { period: `P${months}M`, enforcementNumber });
}

/** "6 months", "13 months", "1 month" from the feature files, in calendar months. */
export function monthsIn(age: string): number {
  const match = /^(\d+) months?$/.exec(age.trim());
  if (!match) {
    throw new Error(`Cannot read "${age}" as a number of months`);
  }
  return Number(match[1]);
}
