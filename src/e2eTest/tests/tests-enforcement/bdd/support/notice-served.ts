// CCD caps event names at 30 characters, so `MARK_NOTICE_SERVED` is the event "Mark eviction notice served".
export const MARK_NOTICE_SERVED = 'Mark notice of eviction as served';

import { Page, expect } from '@playwright/test';
import { ccdDateTime, submitEventAs } from './event-api';
import { fillDateTimeField } from './date-time-field';
import { CLAIMANT_LEGAL_REPRESENTATIVE, expectSignedInAs, requireEmailFor, signInAs } from './session';
import { LONG_TIMEOUT, VERY_LONG_TIMEOUT } from 'playwright.config';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';

export const noticeServed = {
  eventName: MARK_NOTICE_SERVED,
  dropDownLabel: 'Mark eviction notice served',
  pageLabel: "Mark 'notice of eviction' as served",
  servedDateQuestion: "When did you serve the 'notice of eviction'?",
  servedHowQuestion: "How did you serve the 'notice of eviction'?",
  addNoteQuestion: 'Do you want to add a bailiff note?',
  noteLabel: 'Add note',
  confirmationHeader: "'Notice of eviction' marked as served",
  errors: {
    dateNeeded: "Enter when you served the 'notice of eviction'",
    howNeeded: "Enter how you served the 'notice of eviction'",
    addNoteNeeded: 'Select if you want to add a bailiff note',
    noteNeeded: 'Enter a note',
    servedInTheFuture: "The date and time you served the 'notice of eviction' cannot be in the future",
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

export async function ensureOnNoticeServedPage(page: Page): Promise<void> {
  if (await page.locator(noticeServed.fields.servedHow).isVisible().catch(() => false)) {
    return;
  }
  const { openCaseSummary, startEvent } = await import('./enforcement-journeys');
  await openCaseSummary(page);
  await startEvent(MARK_NOTICE_SERVED);
  await expect(page.locator(noticeServed.fields.servedHow)).toBeVisible({ timeout: LONG_TIMEOUT });
}

/** Yesterday at 09:00: the validator refuses a served time in the future. */
export async function enterServedYesterday(page: Page): Promise<void> {
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  await fillDateTimeField(page, noticeServed.fields.servedAt, {
    day: String(yesterday.getDate()),
    month: String(yesterday.getMonth() + 1),
    year: String(yesterday.getFullYear()),
    hour: '09',
    minute: '00',
  });
}

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

export const DEFAULT_SERVED_HOW = 'By hand to the occupant';

export async function enterServedAt(page: Page, date: string, time: string): Promise<void> {
  const { resolveEvictionDate, resolveEvictionTime } = await import('./schedule-eviction');
  await fillDateTimeField(page, noticeServed.fields.servedAt, {
    ...(resolveEvictionDate(date) ?? {}),
    ...(resolveEvictionTime(time) ?? {}),
  });
}

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
