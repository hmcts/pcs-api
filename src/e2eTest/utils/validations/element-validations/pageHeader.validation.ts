import { Page, expect } from '@playwright/test';
import { IValidation, ValidationData } from '../../interfaces/validation.interface';

export class PageHeaderValidation implements IValidation {
  async validate(page: Page, _fieldName: string, data: ValidationData): Promise<void> {
    const locator = page.locator('h1.govuk-heading-xl');

    if (!data) {
      throw new Error('PageHeaderValidation requires a value');
    }

    await expect(locator).toHaveText(String(data));
  }
}
