/**
 * The `markNoticeOfEvictionServed` event (feature 08).
 *
 * One page, reachable once an eviction has been scheduled without dispensing with service. The served
 * date-time is a CCD DateTime, so XUI renders six inputs; the validation messages come from
 * `EvictionScheduleValidator` as mid-event errors rather than CCD field errors.
 *
 * `MARK_NOTICE_SERVED` is the name the feature files and `xuiEvents` use. CCD caps event names at 30
 * characters, so the event itself is called "Mark eviction notice served".
 */
export const MARK_NOTICE_SERVED = 'Mark notice of eviction as served';

import { Page, expect } from '@playwright/test';
import { ccdDateTime, submitEventAs } from './event-api';
import { CLAIMANT_LEGAL_REPRESENTATIVE, expectSignedInAs, requireEmailFor, signInAs } from './session';
import { LONG_TIMEOUT, VERY_LONG_TIMEOUT } from 'playwright.config';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';

export const noticeServed = {
  eventName: MARK_NOTICE_SERVED,
  /** What XUI shows in the next-step drop down. */
  dropDownLabel: 'Mark eviction notice served',
  pageLabel: "Mark 'notice of eviction' as served",
  servedDateQuestion: "When did you serve the 'notice of eviction'?",
  servedHowQuestion: "How did you serve the 'notice of eviction'?",
  addNoteQuestion: 'Do you want to add a bailiff note?',
  noteLabel: 'Add note',
  confirmationHeader: "'Notice of eviction' marked as served",
  /** EvictionScheduleValidator's messages. */
  errors: {
    dateNeeded: "Enter when you served the 'notice of eviction'",
    howNeeded: "Enter how you served the 'notice of eviction'",
    addNoteNeeded: 'Select if you want to add a bailiff note',
    noteNeeded: 'Enter a note',
  },
  servedHowMaxLength: 500,
  /** CCD's own max-length message, e.g. `"xxx..." (501 characters) exceeds the maximum length of 500`. */
  tooLongError: 'exceeds the maximum length of 500',
  fields: {
    servedAt: '#noticeOfEvictionServedAt',
    servedHow: '#noticeOfEvictionServedHow',
    addNote: '#noticeOfEvictionAddBailiffNote',
    noteText: '#bailiffNoteText',
  },
};

/**
 * Opens the event if the page is not already showing it. Some of feature 08's scenarios go straight to an
 * answer step without a navigation step of their own.
 */
export async function ensureOnNoticeServedPage(page: Page): Promise<void> {
  if (await page.locator(noticeServed.fields.servedHow).isVisible().catch(() => false)) {
    return;
  }
  const { openCaseSummary, startEvent } = await import('./enforcement-journeys');
  await openCaseSummary(page);
  await startEvent(MARK_NOTICE_SERVED);
  await expect(page.locator(noticeServed.fields.servedHow)).toBeVisible({ timeout: LONG_TIMEOUT });
}

/**
 * Fills the served date-time with yesterday at 09:00: the validator refuses a time in the future, and nothing
 * can control the app's clock.
 */
export async function enterServedYesterday(page: Page): Promise<void> {
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  const parts: Record<string, string> = {
    day: String(yesterday.getDate()),
    month: String(yesterday.getMonth() + 1),
    year: String(yesterday.getFullYear()),
    hour: '09',
    minute: '00',
  };
  for (const [part, value] of Object.entries(parts)) {
    await page.locator(`${noticeServed.fields.servedAt}-${part}`).fill(value);
  }
}

/**
 * Has the bailiff mark the notice of eviction as served through CCD's data store API, with the XUI journey's
 * answers: served yesterday at 09:00, by hand, with a bailiff note if one is given. Needs an eviction scheduled
 * without dispensing with service. Expects the claimant's legal representative next.
 */
export async function markNoticeServedAsBailiff(world: EnforcementWorld, bailiffNote?: string): Promise<void> {
  const confirmation = await submitEventAs(requireEmailFor('bailiff'), 'markNoticeOfEvictionServed', {
    noticeOfEvictionServedAt: ccdDateTime(-1, '09:00'),
    noticeOfEvictionServedHow: DEFAULT_SERVED_HOW,
    noticeOfEvictionAddBailiffNote: bailiffNote ? 'Yes' : 'No',
    ...(bailiffNote ? { bailiffNoteText: bailiffNote } : {}),
  });
  expect(confirmation.header, 'the "Mark notice of eviction as served" confirmation').toContain(noticeServed.confirmationHeader);
  recordNoticeServed(world, bailiffNote);
  expectSignedInAs(CLAIMANT_LEGAL_REPRESENTATIVE);
}

/**
 * The bailiff's journey in XUI, for a scenario whose When is marking the notice served: the same answers as
 * `markNoticeServedAsBailiff`, then check your answers and the confirmation. Expects the claimant's legal
 * representative next, without signing them in.
 */
export async function markNoticeServedInXui(page: Page, world: EnforcementWorld, bailiffNote?: string): Promise<void> {
  await signInAs('bailiff', page);
  await ensureOnNoticeServedPage(page);
  await enterServedYesterday(page);
  await page.locator(noticeServed.fields.servedHow).fill(DEFAULT_SERVED_HOW);
  await page.locator(`${noticeServed.fields.addNote}_${bailiffNote ? 'Yes' : 'No'}`).check();
  if (bailiffNote) {
    await page.locator(noticeServed.fields.noteText).fill(bailiffNote);
  }
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await page.getByRole('button', { name: 'Save and continue', exact: true }).click();
  await expect(page.getByRole('heading', { name: noticeServed.confirmationHeader }))
    .toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  recordNoticeServed(world, bailiffNote);
  expectSignedInAs(CLAIMANT_LEGAL_REPRESENTATIVE);
}

function recordNoticeServed(world: EnforcementWorld, bailiffNote: string | undefined): void {
  world.noticeServed = true;
  if (bailiffNote) {
    world.enteredNote = bailiffNote;
  }
}

/** What the suite answers to "How did you serve the 'notice of eviction'?" when the scenario does not say. */
export const DEFAULT_SERVED_HOW = 'By hand to the occupant';

/** Fills the served date-time's inputs from a "dd/MM/yyyy" date and an "HH:mm" time. */
export async function enterServedAt(page: Page, date: string, time: string): Promise<void> {
  const { resolveEvictionDate, resolveEvictionTime } = await import('./schedule-eviction');
  const parts: Record<string, string> = { ...(resolveEvictionDate(date) ?? {}), ...(resolveEvictionTime(time) ?? {}) };
  for (const part of ['day', 'month', 'year', 'hour', 'minute']) {
    await page.locator(`${noticeServed.fields.servedAt}-${part}`).fill(parts[part] ?? '');
  }
}

/**
 * Answers whichever of the page's mandatory questions the scenario left blank, so that a scenario about one
 * answer can get past the page: served yesterday at 09:00, by hand, and no bailiff note.
 */
export async function answerRemainingNoticeServedQuestions(page: Page): Promise<void> {
  if (!(await page.locator(`${noticeServed.fields.servedAt}-year`).inputValue())) {
    await enterServedYesterday(page);
  }
  const servedHow = page.locator(noticeServed.fields.servedHow);
  if (!(await servedHow.inputValue())) {
    await servedHow.fill(DEFAULT_SERVED_HOW);
  }
  const addNote = (answer: string) => page.locator(`${noticeServed.fields.addNote}_${answer}`);
  if (!(await addNote('Yes').isChecked()) && !(await addNote('No').isChecked())) {
    await addNote('No').check();
  }
}

/**
 * Submits the event from wherever the scenario left it: the question page, whose blank answers are filled
 * first, or CCD's check-your-answers. Waits for the confirmation.
 */
export async function submitNoticeServed(page: Page, world: EnforcementWorld): Promise<void> {
  if (await page.locator(noticeServed.fields.servedHow).isVisible().catch(() => false)) {
    await answerRemainingNoticeServedQuestions(page);
    await page.getByRole('button', { name: 'Continue', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Check your answers' })).toBeVisible({ timeout: LONG_TIMEOUT });
  }
  await page.getByRole('button', { name: 'Save and continue', exact: true }).click();
  await expect(page.getByRole('heading', { name: noticeServed.confirmationHeader }))
    .toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  world.noticeServed = true;
  world.currentEvent = undefined;
}
