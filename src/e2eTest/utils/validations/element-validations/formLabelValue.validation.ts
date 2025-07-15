import { Page, expect, Locator } from '@playwright/test';
import { IValidation, ValidationData } from '../../interfaces/validation.interface';

export class FormLabelValueValidation implements IValidation {
  async validate(page: Page, fieldName: string, data?: ValidationData): Promise<void> {
    const valueLocator = await this.findFieldValueLocator(page, fieldName);

    if (data?.value !== undefined) {
      await expect(valueLocator).toHaveText(String(data.value));
    } else {
      const value = await valueLocator.textContent();
      if (!value?.trim()) {
        throw new Error(`Value for "${fieldName}" is empty`);
      }
    }
  }

  private async findFieldValueLocator(page: Page, fieldName: string): Promise<Locator> {
    const locators = [
      page.locator(`.case-viewer-label:has-text("${fieldName}")`)
        .locator('xpath=../following-sibling::td[1]')
        .locator('.text-16 span'),

      page.locator(`th#complex-panel-simple-field-label > span.text-16:has-text("${fieldName}")`)
        .locator('xpath=../..')
        .locator('td span.text-16:not(:has(ccd-field-read-label))')
    ];

    for (const locator of locators) {
      if (await locator.count() === 1) {
        await expect(locator).toBeVisible();
        return locator;
      }
    }
    throw new Error(`Field "${fieldName}" not found`);
  }
}
