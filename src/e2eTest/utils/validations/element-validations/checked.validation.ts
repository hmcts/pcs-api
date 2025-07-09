// validations/checked.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class CheckedValidation implements IValidation {
  async validate(page: Page, data: ValidationData, fieldName?: string, groupName?: string): Promise<void> {
    const locator = page.locator(`[data-testid="${fieldName}"]`);

    if ('checked' in data) {
      if (data.checked) {
        await expect(locator).toBeChecked();
      } else {
        await expect(locator).not.toBeChecked();
      }
    } else {
      throw new Error('CheckedValidation requires "checked" property in data');
    }
  }
}
