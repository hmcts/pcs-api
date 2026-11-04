// XUI holds every part of a Date or DateTime field in one form control and revalidates it on each keystroke, so
// a field filled part by part shows "The data entered is not valid" until the last part is in.
import { Locator, Page, expect } from '@playwright/test';
import { fillDateParts } from '@utils/demo-actions';
import { LONG_TIMEOUT } from 'playwright.config';

/** The parts the suite answers. `second` is left at the 0 XUI pre-fills it with. */
export const DATE_TIME_PARTS = ['day', 'month', 'year', 'hour', 'minute'] as const;

export const datePartInput = (page: Page, field: string, part: string): Locator => page.locator(`${field}-${part}`);

export async function fillDateTimeField(
  page: Page,
  field: string,
  values: Record<string, string | undefined>
): Promise<void> {
  const parts = DATE_TIME_PARTS.map(part => ({ input: datePartInput(page, field, part), value: values[part] ?? '' }));
  await expect(parts[0].input).toBeVisible({ timeout: LONG_TIMEOUT });
  await fillDateParts(parts);
}
