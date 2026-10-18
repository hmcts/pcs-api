/**
 * The error summaries XUI renders on an event page: the GOV.UK summary listing field validation errors
 * (`.govuk-error-summary`), and the callback "Errors" and event-failure boxes (`.error-summary`). Every error
 * message the page shows is in one of them; the field-level copies under each input are not.
 */
import { Page } from '@playwright/test';
import { LONG_TIMEOUT } from 'playwright.config';
import { expect } from '../fixtures/enforcement.fixtures';

export const errorSummaries = (page: Page) => page.locator('.govuk-error-summary, .error-summary');

/**
 * Waits for the page's error summaries, then asserts `message` is listed exactly once across them, so an error
 * that is reported twice (by XUI and by the callback, or twice by the callback) fails rather than passing on the
 * first copy.
 */
export async function expectSingleErrorMessage(page: Page, message: string | RegExp, timeout = LONG_TIMEOUT): Promise<void> {
  await expect(errorSummaries(page).first()).toBeVisible({ timeout });
  await expect(errorSummaries(page).getByText(message), `"${message}" listed once in the error summary`).toHaveCount(1);
}
