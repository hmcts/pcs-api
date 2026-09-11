import { Page, expect, Locator } from '@playwright/test';
import { IValidation, validationData } from '../../interfaces/validation.interface';
import { anyOf, waitForInteractive } from '@utils/common/locator.utils';
import { MEDIUM_TIMEOUT } from '../../../playwright.config';

export class InputErrorValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data: validationData): Promise<void> {

    const valueLocator = await this.findFieldValueLocator(page, fieldName, data);

    if (data !== undefined) {
      // Bounded for the same reason as errorMessage: this runs inside 60s `toPass` loops
      // alongside a click that can take the full 40s actionTimeout, so the 30s global
      // default made the wrapper's budget unachievable. See error-message.validation.ts.
      await expect(valueLocator).toHaveText(String(data), { timeout: MEDIUM_TIMEOUT });
    } else {
      const value = await valueLocator.textContent();
      if (!value?.trim()) {
        throw new Error(`Value for "${fieldName}" is empty`);
      }
    }
  }

  private async findFieldValueLocator(page: Page, fieldName: string, data: validationData): Promise<Locator> {
    const locators = [
      // Normal fields
      page.locator(
        `//span[normalize-space()="${fieldName}"]/ancestor::div[contains(@class,'form-group')][1]//span[contains(@class,'error-message')]`
      ),
      // Date fields
      page.locator(
        `//legend//span[normalize-space()="${fieldName}"]/following::span[contains(@class,'error-message')][1]`
      )
    ];


    // count() does not poll, and errors render a tick after submit.
    await waitForInteractive(anyOf(...locators));

    // Ordered strategies: normal fields, then date fields whose label sits in a <legend>. Every
    // strategy is tried before failing, so the date-field fallback is reachable.
    const attempts: string[] = [];
    for (const locator of locators) {
      const count = await locator.count();
      if (count === 0) {
        attempts.push('0 matches');
        continue;
      }

      for (let i = 0; i < count; i++) {
        const item = locator.nth(i);
        if (await item.isVisible()) {
          return item;
        }
      }
      attempts.push(`${count} match(es), none visible`);
    }
    throw new Error(`The error message "${data}" is not triggered for "${fieldName}" `
      + `(strategies tried: ${attempts.join('; ')})`);
  }
}
