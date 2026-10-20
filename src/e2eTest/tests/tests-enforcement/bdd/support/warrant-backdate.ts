// Backdates a warrant's generation date, so the 12 calendar month free repeat period can be tested without
// waiting a year.
import { expect } from '@playwright/test';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { backdateEnforcement } from './backdate-enforcement';
import { requireCaseReference } from './enforcement-journeys';

export async function backdateWarrantGeneration(world: EnforcementWorld, months: number): Promise<void> {
  const enforcementNumber = world.earlierWarrantNumbers.at(-1);
  expect(enforcementNumber, 'a closed warrant on the case to backdate').toBeDefined();
  await backdateEnforcement(requireCaseReference(world), { period: `P${months}M`, enforcementNumber });
}

export function monthsIn(age: string): number {
  const match = /^(\d+) months?$/.exec(age.trim());
  if (!match) {
    throw new Error(`Cannot read "${age}" as a number of months`);
  }
  return Number(match[1]);
}
