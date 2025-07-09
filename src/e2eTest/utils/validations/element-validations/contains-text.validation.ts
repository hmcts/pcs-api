// validations/contains-text.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class ContainsTextValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData): Promise<void> {
    const locator = page.locator(`[data-testid="${fieldName}"]`);

    if ('text' in data) {
      await expect(locator).toContainText(String(data.text));
    } else {
      throw new Error('ContainsTextValidation requires "text" property in data');
    }
  }
}
