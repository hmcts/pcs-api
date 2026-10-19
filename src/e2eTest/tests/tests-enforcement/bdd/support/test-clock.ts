/**
 * Setting pcs-api's clock through testing support (`POST /testing-support/clock`), so that the scenarios that
 * need a date or time in the past or the future can say what "now" is instead of quoting a date and hoping.
 *
 * Everything pcs-api measures against "now" reads the clock this moves: the eviction date must be today or in
 * the future, the served date and the recorded eviction cannot be in the future, the claimant's 72 hour
 * confirmation deadline and the 12 calendar month free repeat warrant period.
 *
 * Two things it does not move. CCD keeps its own clock, so the dates CCD stamps on the case (its history, its
 * created date) stay on the real time. And pcs-api queues its db-scheduler tasks from the real time, so a
 * task scheduled while the clock is moved forwards waits for the real time to catch up; moving the clock
 * backwards is always safe.
 *
 * The offset is process wide, not per case or per user, so a scenario that moves the clock cannot run
 * alongside one that depends on the real date. `resetClockIfMoved` puts it back after every scenario that
 * moved it (fixtures/enforcement.fixtures.ts), and the scenarios that move it are tagged `@clock` so that the
 * suite runs them on their own, on a single worker (scripts/run-enforcement-bdd.ts).
 *
 * The endpoint only exists where pcs-api is run with `ENABLE_TESTING_SUPPORT_CLOCK`: the local cftlib stack
 * (build.gradle) and a per-PR preview (charts/pcs-api/values.preview.template.yaml). AAT is shared, so it
 * keeps the real clock and the `@clock` scenarios are not run there.
 */
import Axios from 'axios';
import { expect } from '@playwright/test';
import { resolveEvictionDate, resolveEvictionTime } from './schedule-eviction';

type SetClockRequest = {
  /** ISO-8601 instant, e.g. `2026-07-08T09:00:00Z`. */
  instant?: string;
  /** ISO-8601 local date and time in UK time, e.g. `2026-07-08T10:00:00`. */
  dateTime?: string;
  /** ISO-8601 duration from the real time, e.g. `PT73H` or `-PT24H`. */
  offset?: string;
};

type ClockState = { now: string; ukDateTime: string; offset: string };

let clockMoved = false;

const pad = (value: string) => value.padStart(2, '0');

function clockUrl(): string {
  return `${process.env.CASE_API_URL}/testing-support/clock`;
}

function serviceAuthHeaders(): Record<string, string> {
  return { ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}` };
}

/** Moves pcs-api's clock, and reports the time it now keeps. */
export async function setClock(request: SetClockRequest): Promise<ClockState> {
  const response = await Axios.post(clockUrl(), request, {
    headers: serviceAuthHeaders(),
    validateStatus: () => true,
  });
  expect(response.status, `setting the pcs-api clock to ${JSON.stringify(request)}`).toBe(200);
  clockMoved = true;
  return response.data as ClockState;
}

/**
 * Sets the clock to a UK date and time as the feature files write them: "08/07/2026" and "10:00", or anything
 * else the eviction date and time steps accept ("tomorrow", "25 June 2026").
 */
export async function setClockToUkDateTime(date: string, time: string): Promise<ClockState> {
  const day = resolveEvictionDate(date);
  const at = resolveEvictionTime(time);
  if (!day || !at) {
    throw new Error(`Cannot set the clock to "${date}" "${time}": both a date and a time are needed`);
  }
  const dateTime = `${day.year}-${pad(day.month)}-${pad(day.day)}T${pad(at.hour)}:${pad(at.minute)}:00`;
  return setClock({ dateTime });
}

/**
 * Puts pcs-api's clock back on the real time, if this worker moved it. The clock is only recorded as back on
 * the real time once the reset has been accepted, so a failed reset is tried again after the next scenario.
 */
export async function resetClockIfMoved(): Promise<void> {
  if (!clockMoved) {
    return;
  }
  const response = await Axios.delete(clockUrl(), {
    headers: serviceAuthHeaders(),
    validateStatus: () => true,
  });
  expect(response.status, 'putting the pcs-api clock back on the real time').toBe(200);
  clockMoved = false;
}
