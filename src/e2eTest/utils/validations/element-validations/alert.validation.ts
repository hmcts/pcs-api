import { Page, test, expect } from '@playwright/test';
import { IValidation, ValidationData } from '../../interfaces/validation.interface';


export class AlertValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData & { description?: string }): Promise<void> {

    const locator = page.locator(`div.${fieldName}`);

    const text = await locator.textContent();
    if (!text || !text.trim()) {
      throw new Error('Alert message not found or empty.');
    }

    const actualText = text.trim();

    await test.step(`Found alert message: "${actualText}"`, async () => {

    const description = data.description ? ` (${data.description})` : '';

    if ('expected' in data) {
      expect(actualText, `Alert text${description} should exactly match`).toBe(String(data.expected));
    } else if ('pattern' in data) {
      const regex = new RegExp(String(data.pattern));
      expect(actualText, `Alert text${description} should match pattern ${regex}`).toMatch(regex);
    } else {
      throw new Error(`AlertValidation requires 'expected' or 'pattern' in data.`);
    }
    });
  }
}
