import { Page, expect } from '@playwright/test';
import { IValidation, validationData } from '../../interfaces/validation.interface';

export class RadioButtonValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: validationData): Promise<void> {

    const label = page.locator(`label.form-label:has-text("${data.option}")`);

    const inputId = await label.getAttribute('for');

    if (!inputId) {
      throw new Error(`No input id found for label "${data.option}"`);
    }

    const input = page.locator(`#${inputId}`);

    const isChecked = await input.isChecked();

    if (data.shouldBeChecked) {
      expect(isChecked).toBe(true);
    } else {
      expect(isChecked).toBe(false);
    }
  }
}
