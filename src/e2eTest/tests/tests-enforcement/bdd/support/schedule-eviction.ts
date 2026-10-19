/**
 * The `scheduleEviction` event (feature 05), reachable once a bailiff is allocated.
 *
 * One page: a date-time field and "Has the court given permission to dispense with service?", then CCD's
 * own check-your-answers (the event uses `showSummary()`). The date-time field is a CCD DateTime, so XUI
 * renders six inputs — `evictionDateTime-day` through `-second`.
 */
import { Page, expect } from '@playwright/test';
import { expectSingleErrorMessage } from './error-summary';
import { fillDateTimeField } from './date-time-field';
import { LONG_TIMEOUT } from 'playwright.config';

export const SCHEDULE_EVICTION = 'Schedule eviction';
export const RESCHEDULE_EVICTION = 'Reschedule eviction';

/** The eviction date and time field, which XUI renders as six inputs. */
export const evictionDateTimeField = '#evictionDateTime';

export const scheduleEviction = {
  pageLabel: 'Schedule eviction',
  dateQuestion: 'Eviction date and time',
  dateHint: 'Use the 24-hour clock. For example, 25 6 2026, 10 00',
  dispenseQuestion: 'Has the court given permission to dispense with service?',
  checkYourAnswers: 'Check your answers',
  saveAndContinue: 'Save and continue',
};

/** The error text each validation case produces, as XUI renders it. */
export const evictionDateErrors = {
  /**
   * XUI says "is required" when the inputs were never filled and "is not valid" when they were filled and
   * then cleared, so the assertion is that the error summary names the field rather than its exact wording.
   */
  needed: scheduleEviction.dateQuestion,
  /** CCD's own DateTime parsing, before the mid-event runs. */
  invalid: 'Date or Time entered is not valid',
  /** EvictionScheduleValidator, surfaced in the callback "Errors" list rather than the error summary. */
  inThePast: 'The eviction date must be today or in the future',
};

const pad = (value: number): string => String(value).padStart(2, '0');

/**
 * Resolves the dates the feature files use. "yesterday" and "tomorrow" are relative so the scenarios do
 * not rot; an explicit `dd/MM/yyyy` or "Thursday 25 June 2026" is taken literally.
 */
export function resolveEvictionDate(date: string): { day: string; month: string; year: string } | undefined {
  if (!date.trim()) {
    return undefined;
  }
  if (date === 'yesterday' || date === 'tomorrow') {
    const when = new Date();
    when.setDate(when.getDate() + (date === 'tomorrow' ? 1 : -1));
    return { day: pad(when.getDate()), month: pad(when.getMonth() + 1), year: String(when.getFullYear()) };
  }
  const slashed = date.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (slashed) {
    return { day: slashed[1], month: slashed[2], year: slashed[3] };
  }
  // "Thursday 25 June 2026" / "25 June 2026"
  const spelled = date.match(/(\d{1,2})\s+([A-Za-z]+)\s+(\d{4})/);
  if (spelled) {
    const month = new Date(`${spelled[2]} 1, 2000`).getMonth();
    if (Number.isNaN(month)) {
      throw new Error(`Unrecognised month in eviction date "${date}"`);
    }
    return { day: pad(Number(spelled[1])), month: pad(month + 1), year: spelled[3] };
  }
  throw new Error(`Unrecognised eviction date "${date}". Use dd/MM/yyyy, "yesterday", "tomorrow" or "25 June 2026"`);
}

/** Splits "10:00" or "25:00" into its parts, leaving them as given so invalid times still reach the app. */
export function resolveEvictionTime(time: string): { hour: string; minute: string } | undefined {
  if (!time.trim()) {
    return undefined;
  }
  const [hour, minute = '00'] = time.split(':');
  return { hour, minute };
}

/**
 * Fills the eviction date and time. An empty date or time leaves those inputs blank so that the
 * "date and time are needed" case exercises the real required-field validation.
 */
export async function enterEvictionDateTime(page: Page, date: string, time: string): Promise<void> {
  await fillDateTimeField(page, evictionDateTimeField, {
    ...(resolveEvictionDate(date) ?? {}),
    ...(resolveEvictionTime(time) ?? {}),
  });
}

export const dispenseWithServiceRadio = (page: Page, answer: string) =>
  page.locator(`#evictionDispenseWithService_${answer}`);

/**
 * Any error the page is showing: the GOV.UK error summary for field-level problems, plus the callback
 * "Errors" list CCD renders separately for a rejected mid-event.
 */
export async function expectEvictionError(page: Page, message: string): Promise<void> {
  await expectSingleErrorMessage(page, message);
}

/** The page rejected the date and told the user, naming the eviction date and time field. */
export async function expectEvictionDateNeeded(page: Page): Promise<void> {
  await expectSingleErrorMessage(page, scheduleEviction.dateQuestion);
  await expect(page.getByRole('heading', { name: scheduleEviction.pageLabel })).toBeVisible();
}

export async function expectCheckYourAnswers(page: Page): Promise<void> {
  await expect(page.getByRole('heading', { name: scheduleEviction.checkYourAnswers })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
}

/** The check-your-answers row for a question, with its value and Change link. */
export const answerRow = (page: Page, question: string) =>
  page.locator('tr, .govuk-summary-list__row').filter({ hasText: question }).first();

/**
 * The read-only "Eviction details" summary list on the schedule and reschedule screens
 * (`EvictionDetailsMarkup`), as key to value. Several names in one row are separated by line breaks.
 */
export async function evictionDetailsRows(page: Page): Promise<Record<string, string>> {
  const heading = page.getByRole('heading', { name: 'Eviction details', exact: true });
  await expect(heading).toBeVisible({ timeout: LONG_TIMEOUT });
  const list = page.locator('dl.govuk-summary-list').filter({ hasText: 'Defendant names' }).first();
  const rows: Record<string, string> = {};
  for (const row of await list.locator('.govuk-summary-list__row').all()) {
    const key = (await row.locator('.govuk-summary-list__key').innerText()).trim();
    rows[key] = (await row.locator('.govuk-summary-list__value').innerText()).trim();
  }
  return rows;
}
