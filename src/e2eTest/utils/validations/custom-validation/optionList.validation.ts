// validations/attribute.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class optionListValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData): Promise<void> {
    if (!Array.isArray(data.allowed)) {
      throw new Error(`RadioOptionsValidation requires "allowed"  in data`);
    }

    const locator = page.locator(`input[type="radio"][name="${fieldName}"]`,);
    const count = await locator.count();
    const actual: string[] = [];

    for (let i = 0; i < count; i++) {
      const value = await locator.nth(i).getAttribute('value');
      if (value) actual.push(value);
    }

    const expected = data.allowed;
    actual.sort();
    expected.sort();

    expect(actual).toEqual(expected);
  }
}
