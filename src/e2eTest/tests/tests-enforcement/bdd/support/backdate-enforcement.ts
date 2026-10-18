/**
 * Moving an enforcement's timestamps into the past through pcs-api testing support
 * (`POST /testing-support/enforcement/{caseReference}/backdate`), for the rules measured from when something
 * happened: the claimant's 72 hours to confirm an eviction and the free repeat warrant within 12 calendar
 * months of the first being generated. Every timestamp the enforcement owns moves by the same amount: the
 * eviction date and time, when it was approved (generated), its status change, notes, allocations, returns
 * and documents.
 */
import Axios from 'axios';
import { expect } from '../fixtures/enforcement.fixtures';

export type Backdate = {
  /** ISO-8601 period, e.g. `P11M` or `P1Y2M3D`. */
  period?: string;
  /** ISO-8601 duration, e.g. `PT73H`. */
  duration?: string;
  /** The warrant or writ number; the case's current enforcement when omitted. */
  enforcementNumber?: string;
};

export async function backdateEnforcement(caseReference: string, backdate: Backdate): Promise<void> {
  const response = await Axios.post(
    `${process.env.CASE_API_URL}/testing-support/enforcement/${caseReference}/backdate`,
    backdate,
    {
      headers: { ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}` },
      validateStatus: () => true,
    }
  );
  expect(response.status, `backdating the enforcement on ${caseReference} by ${JSON.stringify(backdate)}`).toBe(204);
}

/**
 * An age as the feature files write it ("6 months", "13 months", "2 years", "73 hours", "5 days") as the
 * ISO-8601 amount the backdate endpoint takes.
 */
export function ageAsBackdate(age: string): Backdate {
  const match = /^\s*(\d+)\s+(hour|day|week|month|year)s?\s*$/i.exec(age);
  if (!match) {
    throw new Error(`Cannot read "${age}" as an age: expected e.g. "6 months" or "73 hours"`);
  }
  const amount = Number(match[1]);
  switch (match[2].toLowerCase()) {
    case 'hour':
      return { duration: `PT${amount}H` };
    case 'day':
      return { period: `P${amount}D` };
    case 'week':
      return { period: `P${amount * 7}D` };
    case 'month':
      return { period: `P${amount}M` };
    default:
      return { period: `P${amount}Y` };
  }
}
