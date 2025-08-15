import {Page, expect} from '@playwright/test';
import {IValidation} from '../../interfaces/validation.interface';

export class ErrorMessageValidation implements IValidation {
  async validate(page: Page, input: string | Record<string, string>): Promise<void> {
    const checkPair = async (header: string, msg: string) => {
      const h = page.locator('h3.error-summary-heading', { hasText: header });
      await expect(h).toBeVisible();
      const m = h.locator('xpath=following-sibling::*[not(self::h3)][1]');
      await expect(m).toContainText(msg);
    };
    if (typeof input === 'string') {
      const message = page
        .locator('h3.error-summary-heading')
        .locator('xpath=following-sibling::*[not(self::h3)][1]')
        .filter({ hasText: input })
        .first();
      await expect(message).toBeVisible();
      return;
    }
    for (const [header, msg] of Object.entries(input)) {
      await checkPair(header, msg);
    }
  }
}
