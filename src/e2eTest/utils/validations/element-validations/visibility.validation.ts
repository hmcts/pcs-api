// validations/visibility.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class VisibilityValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData): Promise<void> {
    const locator = page.locator(`label:has-text("${fieldName}")`);
    await expect(locator).toBeVisible();
  }
}
