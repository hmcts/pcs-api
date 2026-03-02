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
      page.locator(`//span[text()="${fieldName}"]/ancestor::div[contains(@class,'form-group-error')]//child::span[contains(@class,'error-message')]`)
    ];

    for (const locator of locators) {
      if (await locator.count() === 1 && await locator.isVisible()) {
        return locator;
      }
    }
    throw new Error(`The error message "${data}" is not triggered`);
  }
}