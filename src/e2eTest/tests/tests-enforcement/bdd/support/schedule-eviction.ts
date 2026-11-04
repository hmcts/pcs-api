import { Page, expect } from '@playwright/test';
import { expectSingleErrorMessage } from './error-summary';
import { fillDateTimeField } from './date-time-field';
import { LONG_TIMEOUT } from 'playwright.config';

export const SCHEDULE_EVICTION = 'Schedule eviction';
export const RESCHEDULE_EVICTION = 'Reschedule eviction';

export const evictionDateTimeField = '#evictionDateTime';

export const scheduleEviction = {
  pageLabel: 'Schedule eviction',
  dateQuestion: 'When do you want to schedule the eviction for?',
  dateHint: 'Use the 24-hour clock. For example, use 17:30 instead of 5:30pm.',
  dispenseQuestion: 'Has the court given permission to dispense with service?',
  checkYourAnswers: 'Check your answers',
  saveAndContinue: 'Save and continue',
  evictionDetailsRows: ['Claimant type', 'Claimants', 'Defendants'],
};

export const rescheduleEviction = {
  pageLabel: 'Reschedule eviction',
  dateQuestion: 'When do you want to reschedule the eviction for?',
};

/** The eviction date question differs per event, and XUI builds its required-field error out of it. */
export const evictionDateQuestion = (event: string | undefined): string =>
  event === RESCHEDULE_EVICTION ? rescheduleEviction.dateQuestion : scheduleEviction.dateQuestion;

export const evictionDateErrors = {
  /** XUI says "is required" when the inputs were never filled and "is not valid" when they were cleared, so only the field name is asserted. */
  needed: scheduleEviction.dateQuestion,
  invalid: 'Date or Time entered is not valid',
  inThePast: 'The eviction date must be today or in the future',
};

const pad = (value: number): string => String(value).padStart(2, '0');

const DAY_OFFSETS: Record<string, number> = { yesterday: -1, today: 0, tomorrow: 1 };

/** pcs-api validates these fields against a UK clock, so "now" is read in UK time rather than the machine's. */
function ukNow(offset: { days?: number; minutes?: number }): Record<string, string> {
  const when = new Date();
  when.setUTCDate(when.getUTCDate() + (offset.days ?? 0));
  when.setUTCMinutes(when.getUTCMinutes() + (offset.minutes ?? 0));
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Europe/London',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(when);
  return Object.fromEntries(parts.map(part => [part.type, part.value]));
}

/** "yesterday", "today" and "tomorrow" are relative so the scenarios do not rot; an explicit date is taken literally. */
export function resolveEvictionDate(date: string): { day: string; month: string; year: string } | undefined {
  if (!date.trim()) {
    return undefined;
  }
  const dayOffset = DAY_OFFSETS[date];
  if (dayOffset !== undefined) {
    const { day, month, year } = ukNow({ days: dayOffset });
    return { day, month, year };
  }
  const slashed = date.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (slashed) {
    return { day: slashed[1], month: slashed[2], year: slashed[3] };
  }
  const spelled = date.match(/(\d{1,2})\s+([A-Za-z]+)\s+(\d{4})/);
  if (spelled) {
    const month = new Date(`${spelled[2]} 1, 2000`).getMonth();
    if (Number.isNaN(month)) {
      throw new Error(`Unrecognised month in eviction date "${date}"`);
    }
    return { day: pad(Number(spelled[1])), month: pad(month + 1), year: spelled[3] };
  }
  throw new Error(
    `Unrecognised eviction date "${date}". Use dd/MM/yyyy, "yesterday", "today", "tomorrow" or "25 June 2026"`
  );
}

const MINUTE_OFFSETS: Record<string, number> = { 'one minute from now': 1, 'one minute ago': -1 };

/**
 * "one minute from now" and "one minute ago" are relative; "10:00" or "25:00" are split as given so invalid
 * times still reach the app.
 */
export function resolveEvictionTime(time: string): { hour: string; minute: string } | undefined {
  if (!time.trim()) {
    return undefined;
  }
  const minuteOffset = MINUTE_OFFSETS[time];
  if (minuteOffset !== undefined) {
    const { hour, minute } = ukNow({ minutes: minuteOffset });
    return { hour, minute };
  }
  const [hour, minute = '00'] = time.split(':');
  return { hour, minute };
}

/** An empty date or time leaves those inputs blank, so the required-field validation is the real one. */
export async function enterEvictionDateTime(page: Page, date: string, time: string): Promise<void> {
  await fillDateTimeField(page, evictionDateTimeField, {
    ...(resolveEvictionDate(date) ?? {}),
    ...(resolveEvictionTime(time) ?? {}),
  });
}

export const dispenseWithServiceRadio = (page: Page, answer: string) =>
  page.locator(`#evictionDispenseWithService_${answer}`);

export async function expectEvictionError(page: Page, message: string): Promise<void> {
  await expectSingleErrorMessage(page, message);
}

export async function expectEvictionDateNeeded(page: Page): Promise<void> {
  await expectSingleErrorMessage(page, scheduleEviction.dateQuestion);
  await expect(page.getByRole('heading', { name: scheduleEviction.pageLabel })).toBeVisible();
}

export async function expectCheckYourAnswers(page: Page): Promise<void> {
  await expect(page.getByRole('heading', { name: scheduleEviction.checkYourAnswers })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
}

export const answerRow = (page: Page, question: string) =>
  page.locator('tr, .govuk-summary-list__row').filter({ hasText: question }).first();

export const evictionDetailsList = (page: Page) =>
  page.locator('dl.govuk-summary-list').filter({ hasText: 'Defendants' }).first();

export async function evictionDetailsRows(page: Page): Promise<Record<string, string>> {
  const heading = page.getByRole('heading', { name: 'Eviction details', exact: true });
  await expect(heading).toBeVisible({ timeout: LONG_TIMEOUT });
  const list = evictionDetailsList(page);
  const rows: Record<string, string> = {};
  for (const row of await list.locator('.govuk-summary-list__row').all()) {
    const key = (await row.locator('.govuk-summary-list__key').innerText()).trim();
    rows[key] = (await row.locator('.govuk-summary-list__value').innerText()).trim();
  }
  return rows;
}
