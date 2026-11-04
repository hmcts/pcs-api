// XUI lists field validation errors in `.govuk-error-summary` and callback errors in `.error-summary`.
import { Page } from '@playwright/test';
import { LONG_TIMEOUT } from 'playwright.config';
import { expect } from '../fixtures/enforcement.fixtures';

export const errorSummaries = (page: Page) => page.locator('.govuk-error-summary, .error-summary');

/** Asserts `message` is listed exactly once across them, so an error reported twice fails rather than passing on the first copy. */
export async function expectSingleErrorMessage(page: Page, message: string | RegExp, timeout = LONG_TIMEOUT): Promise<void> {
  await expect(errorSummaries(page).first()).toBeVisible({ timeout });
  await expect(errorSummaries(page).getByText(message), `"${message}" listed once in the error summary`).toHaveCount(1);
}
