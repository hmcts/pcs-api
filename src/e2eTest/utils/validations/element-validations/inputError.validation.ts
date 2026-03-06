import { Page, expect, Locator } from '@playwright/test';
import { IValidation, validationData } from '../../interfaces/validation.interface';

export class InputErrorValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data: validationData): Promise<void> {

    const valueLocator = await this.findFieldValueLocator(page, fieldName, data);

    if (data !== undefined) {
      await expect(valueLocator).toHaveText(String(data));
    } else {
      const value = await valueLocator.textContent();
      if (!value?.trim()) {
        throw new Error(`Value for "${fieldName}" is empty`);
      }
    }
  }

  private async findFieldValueLocator(page: Page, fieldName: string, data: validationData): Promise<Locator> {
    const locators = [
      page.locator(`//span[text()="${fieldName}"]/ancestor::div[contains(@class,'form-group form-group-error')]//child::span[contains(@class,'error-message')]`)
    ];

    for (const locator of locators) {

      const count = await locator.count();  

      if (count === 0) {
        throw new Error(`The error message "${data}" is not triggered (no elements found).`);
      }

      if (count === 1) {
        const item = locator.first();
        if (await item.isVisible()) {
          return item;
        }
        throw new Error(`The error message "${data}" exists but is hidden.`);
      }

      for (let i = 0; i < count; i++) {
        const item = locator.nth(i);
        if (await item.isVisible()) {
          return item;
        }
      }

    }
    throw new Error(`The error message "${data}" is not triggered`);
  }
}