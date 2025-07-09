// validations/count.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class CountValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData): Promise<void> {
    const locator = page.locator(`[data-testid="${fieldName}"]`);

    if ('count' in data) {
      await expect(locator).toHaveCount(Number(data.count));
    } else {
      throw new Error('CountValidation requires "count" property in data');
    }
  }
}
