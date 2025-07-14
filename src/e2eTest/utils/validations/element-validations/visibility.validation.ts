import { Page, expect } from '@playwright/test';
import { IValidation, ValidationData } from '../../interfaces/validation.interface';

export class VisibilityValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData): Promise<void> {
    const locator = page.locator(`:has-text("${data.visible}") + :is(td, div, a)`);
    console.log("data present in object"+data.visible);
    if ('visible' in data) {
      if (data.visible) {
        await expect(locator).toBeVisible();
      } else {
        await expect(locator).toBeHidden();
      }
    } else {
      throw new Error('VisibilityValidation requires "visible" property in data');
    }
  }
}
