/**
 * Moving a warrant's generation date into the past, so the 12 calendar month free repeat warrant period
 * (HDPI-8424) can be tested without waiting a year.
 *
 * Calls `POST /testing-support/enforcement/{caseReference}/backdate`, which is being added separately. The
 * request body below is this suite's assumption and must be reconciled with that endpoint: it names the
 * warrant by number and gives the UK date it should have been generated (approved) on.
 */
import Axios from 'axios';
import { expect } from '@playwright/test';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { requireCaseReference } from './enforcement-journeys';

/** `date` minus `months` calendar months, as an ISO local date (YYYY-MM-DD). */
export function calendarMonthsBefore(months: number, date = new Date()): string {
  const target = new Date(date.getFullYear(), date.getMonth() - months, 1);
  const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate();
  target.setDate(Math.min(date.getDate(), lastDay));
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${target.getFullYear()}-${pad(target.getMonth() + 1)}-${pad(target.getDate())}`;
}

/** Backdates the generation of the warrant this scenario last closed to `months` calendar months ago. */
export async function backdateWarrantGeneration(world: EnforcementWorld, months: number): Promise<void> {
  const enforcementNumber = world.earlierWarrantNumbers.at(-1);
  expect(enforcementNumber, 'a closed warrant on the case to backdate').toBeDefined();
  const response = await Axios.post(
    `${process.env.CASE_API_URL}/testing-support/enforcement/${requireCaseReference(world)}/backdate`,
    { enforcementNumber, warrantGeneratedOn: calendarMonthsBefore(months) },
    {
      headers: {
        ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
        'Content-Type': 'application/json',
      },
      validateStatus: () => true,
    }
  );
  expect(response.status, `backdating ${enforcementNumber}: ${JSON.stringify(response.data)}`).toBe(204);
}

/** "6 months", "13 months", "1 month" from the feature files, in calendar months. */
export function monthsIn(age: string): number {
  const match = /^(\d+) months?$/.exec(age.trim());
  if (!match) {
    throw new Error(`Cannot read "${age}" as a number of months`);
  }
  return Number(match[1]);
}
