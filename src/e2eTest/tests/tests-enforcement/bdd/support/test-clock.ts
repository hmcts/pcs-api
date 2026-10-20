// The clock offset is process wide, so a scenario that moves it cannot run alongside one that depends on the
// real date: those are tagged `@clock` and run on their own worker. CCD keeps its own clock regardless.
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

export async function setClock(request: SetClockRequest): Promise<ClockState> {
  const response = await Axios.post(clockUrl(), request, {
    headers: serviceAuthHeaders(),
    validateStatus: () => true,
  });
  expect(response.status, `setting the pcs-api clock to ${JSON.stringify(request)}`).toBe(200);
  clockMoved = true;
  return response.data as ClockState;
}

export async function setClockToUkDateTime(date: string, time: string): Promise<ClockState> {
  const day = resolveEvictionDate(date);
  const at = resolveEvictionTime(time);
  if (!day || !at) {
    throw new Error(`Cannot set the clock to "${date}" "${time}": both a date and a time are needed`);
  }
  const dateTime = `${day.year}-${pad(day.month)}-${pad(day.day)}T${pad(at.hour)}:${pad(at.minute)}:00`;
  return setClock({ dateTime });
}

/** The clock is only recorded as back on the real time once the reset has been accepted, so a failed reset is tried again. */
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
