import { Locator, Page } from '@playwright/test';

import { LONG_TIMEOUT, SHORT_TIMEOUT } from '../../playwright.config';

/**
 * Waits for the loading spinner to go. A bare `waitFor({ state: 'detached' })` inherits the 40s
 * `actionTimeout` and throws, so a spinner that lingers costs the full timeout and then fails the
 * test. The spinner going is never the assertion — the caller's next step reports the real error.
 */
export async function waitForSpinner(page: Page, timeout: number = LONG_TIMEOUT): Promise<void> {
  await page
    .locator('.spinner-container')
    .waitFor({ state: 'detached', timeout })
    .catch(() => undefined);
}

/** Matches whichever of the given locators is present. */
export function anyOf(...locators: Locator[]): Locator {
  if (locators.length === 0) {
    throw new Error('anyOf requires at least one locator');
  }
  return locators
    .map(locator => locator.first())
    .reduce((combined, next) => combined.or(next))
    .first();
}

/**
 * Waits for `locator` before a non-retrying probe such as `count()` or `isChecked()`.
 * Those read the DOM instantly, so after a navigation they can see the previous page.
 * A timeout is ignored: the caller's action auto-waits and reports the real error.
 */
export async function waitForInteractive(locator: Locator, timeout: number = SHORT_TIMEOUT): Promise<void> {
  await locator
    .first()
    .waitFor({ state: 'visible', timeout })
    .catch(() => undefined);
}
