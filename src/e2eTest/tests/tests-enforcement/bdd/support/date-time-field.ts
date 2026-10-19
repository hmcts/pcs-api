/**
 * The inputs XUI renders for a CCD Date or DateTime field: one per part, each with the field's id followed by
 * the part's name, e.g. `#evictionDateTime-day` through `#evictionDateTime-second`.
 *
 * All the parts belong to one form control, which XUI revalidates on every keystroke, so a field filled part
 * by part shows "The data entered is not valid" from the first part until the last. `fillDateParts` keeps that
 * off the demo videos by setting the parts together; outside the demo each part is filled in turn as before.
 */
import { Locator, Page, expect } from '@playwright/test';
import { fillDateParts } from '@utils/demo-actions';
import { LONG_TIMEOUT } from 'playwright.config';

/** The parts the suite answers. `second` is left at the 0 XUI pre-fills it with. */
export const DATE_TIME_PARTS = ['day', 'month', 'year', 'hour', 'minute'] as const;

export const datePartInput = (page: Page, field: string, part: string): Locator => page.locator(`${field}-${part}`);

/**
 * Fills the date or date-time field with id `field` from `values`, keyed by part. A part `values` does not
 * give is left blank, so that the scenarios about a missing date and time still reach the app without one.
 */
export async function fillDateTimeField(
  page: Page,
  field: string,
  values: Record<string, string | undefined>
): Promise<void> {
  const parts = DATE_TIME_PARTS.map(part => ({ input: datePartInput(page, field, part), value: values[part] ?? '' }));
  await expect(parts[0].input).toBeVisible({ timeout: LONG_TIMEOUT });
  await fillDateParts(parts);
}
