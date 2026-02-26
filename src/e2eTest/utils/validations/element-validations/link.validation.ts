import { Page, expect } from '@playwright/test';
import { IValidation, validationRecord } from '@utils/interfaces';
import { escapeForRegex } from '@utils/common/string.utils';

/**
 * Validates that a link with the given text is visible.
 * Uses xpath to exclude links inside hidden ancestors (e.g. duplicate content in hidden blocks).
 */
export class LinkValidation implements IValidation {
  async validate(
    page: Page,
    _validation: string,
    _fieldName?: validationRecord | string | number | boolean | string[] | object,
    data?: validationRecord | string | number | boolean | string[] | object
  ): Promise<void> {
    const text = this.getLinkText(data);
    const regex = new RegExp('^\\s*' + escapeForRegex(text) + '\\s*$');
    const locator = page
      .locator('xpath=//a[not(ancestor::*[@hidden])]')
      .filter({ hasText: regex })
      .first();
    await locator.scrollIntoViewIfNeeded();
    await locator.waitFor({ state: 'visible' });
    const actual = await locator.innerText();
    const normalized = (s: string) => (s ?? '').replace(/\s+/g, ' ').trim();
    expect(normalized(actual ?? ''), `Expected link text "${text}"`).toBe(normalized(text));
  }

  private getLinkText(data: unknown): string {
    if (data && typeof data === 'object' && 'text' in data && typeof (data as { text: unknown }).text === 'string') {
      return (data as { text: string }).text;
    }
    throw new Error('Link validation requires data with "text" (e.g. { text: "link text" })');
  }
}
