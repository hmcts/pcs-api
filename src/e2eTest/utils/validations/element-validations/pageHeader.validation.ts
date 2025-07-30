import { Page, expect } from '@playwright/test';
import { IValidation, ValidationData } from '../../interfaces/validation.interface';

export class MainHeaderValidation implements IValidation {
  async validate(page: Page, _fieldName: string, data: ValidationData): Promise<void> {

    if (!data || !data.expected) {
      throw new Error('PageHeaderValidation requires a value');
    }

    const locator = page.locator('h1,h1.govuk-heading-xl, h1.govuk-heading-l');

    // ✅ Wait for the <h1> to contain the expected text
    await expect(locator).toHaveText(String(data.expected), { timeout: 10000 });

    // Optional: If you still want to manually compare
    const text = (await locator.textContent())?.trim();
    expect(text).toBe(String(data.expected));
  }
}
