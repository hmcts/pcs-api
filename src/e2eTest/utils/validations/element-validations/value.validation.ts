// validations/value.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class ValueValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData): Promise<void> {
    const locator = page.locator(`[data-testid="${fieldName}"]`);

    if ('expected' in data) {
      await expect(locator).toHaveValue(String(data.expected));
    } else {
      throw new Error('ValueValidation requires "expected" property in data');
    }
  }
}
