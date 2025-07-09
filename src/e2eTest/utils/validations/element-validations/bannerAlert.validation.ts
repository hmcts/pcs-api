// validations/banner-alert.validation.ts
import { Page, expect, test } from '@playwright/test';
import { IValidation, ValidationData } from '../../interfaces/validation.interface';

export class BannerAlertValidation implements IValidation {
  async validate(page: Page, data: ValidationData, fieldName?: string, groupName?: string): Promise<void> {
    const locator = page.locator('div.alert-message');
    //const locator = page.locator('div.alert-message', { hasText: message });

      const alertText = (await locator.textContent())?.trim();

      if (!alertText) {
        throw new Error('Alert message not found or empty.');
      }

      await test.step(`Found alert message: "${alertText}"`, async () => {
        if (!('message' in data)) {
          throw new Error('BannerAlertValidation requires "message" property in data.');
        }

        const message = String(data.message);

        const isPattern =
          message.includes('.*') ||
          message.startsWith('^') ||
          message.endsWith('$');

        if (isPattern) {
          const regex = new RegExp(message);
          expect(alertText, `Alert should match pattern: ${regex}`).toMatch(regex);
        } else {
          expect(alertText, `Alert should exactly match`).toBe(message);
        }
      });
  }
}
