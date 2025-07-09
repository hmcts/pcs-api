// validations/attribute.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class AttributeValidation implements IValidation {
  async validate(page: Page, data: ValidationData, fieldName?: string, groupName?: string): Promise<void> {
    const locator = page.locator(`[data-testid="${fieldName}"]`);

    if ('attribute' in data && 'value' in data) {
      await expect(locator).toHaveAttribute(String(data.attribute), String(data.value));
    } else {
      throw new Error('AttributeValidation requires "attribute" and "value" properties in data');
    }
  }
}
