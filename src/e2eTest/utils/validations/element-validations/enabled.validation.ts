// validations/enabled.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class EnabledValidation implements IValidation {
  async validate(page: Page, data: ValidationData, fieldName?: string, groupName?: string): Promise<void> {
    const locator = page.locator(`[data-testid="${fieldName}"]`);

    if ('enabled' in data) {
      if (data.enabled) {
        await expect(locator).toBeEnabled();
      } else {
        await expect(locator).toBeDisabled();
      }
    } else {
      throw new Error('EnabledValidation requires "enabled" property in data');
    }
  }
}
