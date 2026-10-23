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
      // Normal fields
      page.locator(
        `//span[normalize-space()="${fieldName}"]/ancestor::div[contains(@class,'form-group')][1]//span[contains(@class,'error-message')]`
      ),
      // Date fields
      page.locator(
        `//legend//span[normalize-space()="${fieldName}"]/following::span[contains(@class,'error-message')][1]`
      ),

      page.locator(
        `//label[normalize-space()="${fieldName}"]/ancestor::fieldset[1]//span[contains(@class,'error-message')]`
      ),
    ];


    for (const locator of locators) {

      const count = await locator.count();

      if (count === 0) {
        continue;
      }

      for (let i = 0; i < count; i++) {
        const item = locator.nth(i);

        if (await item.isVisible()) {
          return item;
        }
      }
    }

    throw new Error(
      `The error message "${JSON.stringify(data)}" is not triggered or not visible.`
    );
  }
}
