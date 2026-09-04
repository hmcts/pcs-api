import { Locator, Page } from '@playwright/test';

import { SHORT_TIMEOUT } from '../../playwright.config';
import { exactTextWithOptionalWhitespaceRegex } from './string.utils';

const HEADING_SELECTOR = 'h1,h1.govuk-heading-xl, h1.govuk-heading-l, h1.govuk-panel__title';

// Scoped by text because work-basket pages render extra h1.column-label elements
// ("Priority", …) that can precede the real heading and win a bare .first().
export function pageHeading(page: Page, expectedText?: string): Locator {
  const visibleHeadings = page.locator(HEADING_SELECTOR).filter({ visible: true });
  return expectedText
    ? visibleHeadings.filter({ hasText: exactTextWithOptionalWhitespaceRegex(expectedText) }).first()
    : visibleHeadings.first();
}

/** Reads the current main heading without throwing on strict-mode or missing elements. */
export async function readPageHeading(page: Page): Promise<string> {
  const text = await pageHeading(page)
    .textContent({ timeout: SHORT_TIMEOUT })
    .catch(() => '');
  return (text ?? '').trim();
}

/** Polls for a main heading with exactly `text`; false if it never appears. */
export async function hasPageHeading(page: Page, text: string, timeout: number = SHORT_TIMEOUT): Promise<boolean> {
  return pageHeading(page, text)
    .waitFor({ state: 'visible', timeout })
    .then(() => true)
    .catch(() => false);
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
