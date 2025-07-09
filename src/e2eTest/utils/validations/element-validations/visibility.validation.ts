// validations/visibility.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class VisibilityValidation implements IValidation {
  async validate(page: Page, data: ValidationData, fieldName?: string, groupName?: string): Promise<void> {
    const locator = page.locator(`[data-testid="${fieldName}"]`);

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
