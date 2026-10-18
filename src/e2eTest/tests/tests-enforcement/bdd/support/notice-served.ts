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
import { LONG_TIMEOUT } from 'playwright.config';

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
