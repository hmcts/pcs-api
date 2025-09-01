import { Page, expect } from '@playwright/test';
import { IValidation, validationData } from '../../interfaces/validation.interface';

export class HeaderValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: validationData): Promise<void> {
    let locator;

    switch (data.elementType) {
      case 'mainHeader':
        locator = page
            .locator('h1, h1.govuk-heading-xl, h1.govuk-heading-l')
            .filter({ hasText: String(data.text) });
        break;

      case 'subHeader':
        locator = page
            .locator('h2, h2.govuk-heading-l, h2.govuk-heading-m')
            .filter({ hasText: String(data.text) });
        break;

      default:
        throw new Error(`Unsupported header type: ${fieldName}`);
    }

    await expect(locator).toHaveText(String(data.text), { timeout: 10000 });
  }
}
