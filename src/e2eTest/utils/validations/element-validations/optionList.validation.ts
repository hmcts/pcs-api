// validations/attribute.validation.ts
import { Page, expect } from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class OptionListValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData): Promise<void> {

    // loop through data.options and find if the element exists in page.locator(`fieldset:has-text("${fieldName}") >> label:has-text("${data.options.option1}")`)
    const locator = page.locator(`input[type="${data.elementType}"][name="${fieldName}"]`,);
    await page.locator(`fieldset:has-text("${fieldName}") >> label:has-text("${data.options}")`).click();
    const count = await locator.count();
    const actual: string[] = [];

    for (let i = 0; i < count; i++) {
      const value = await locator.nth(i).getAttribute('value');
      if (value) actual.push(value);
    }

    const expected = data.options ;
   // actual.sort();
    //expected.sort();

    expect(actual).toEqual(expected);
  }
}
