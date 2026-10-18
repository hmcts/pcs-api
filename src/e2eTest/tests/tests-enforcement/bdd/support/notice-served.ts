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

import { BrowserContext, Page, expect } from '@playwright/test';
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
 * Marks the notice of eviction as served as the bailiff, end to end, with a bailiff note if one is given, and
 * signs back in as the claimant's legal representative. Needs an eviction scheduled without dispensing with
 * service.
 */
export async function markNoticeServedAsBailiff(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  bailiffNote?: string
): Promise<void> {
  const { signInAs } = await import('./enforcement-journeys');
  const { restoreClaimantSession } = await import('./approve-warrant');
  try {
    await signInAs('bailiff', page, context);
    await ensureOnNoticeServedPage(page);
    await enterServedYesterday(page);
    await page.locator(noticeServed.fields.servedHow).fill('By hand to the occupant');
    await page.locator(`${noticeServed.fields.addNote}_${bailiffNote ? 'Yes' : 'No'}`).check();
    if (bailiffNote) {
      await page.locator(noticeServed.fields.noteText).fill(bailiffNote);
    }
    await page.getByRole('button', { name: 'Continue', exact: true }).click();
    await page.getByRole('button', { name: 'Save and continue', exact: true }).click();
    await expect(page.getByRole('heading', { name: noticeServed.confirmationHeader }))
      .toBeVisible({ timeout: VERY_LONG_TIMEOUT });
    world.noticeServed = true;
    if (bailiffNote) {
      world.enteredNote = bailiffNote;
    }
  } finally {
    await restoreClaimantSession(page, context);
  }
}
