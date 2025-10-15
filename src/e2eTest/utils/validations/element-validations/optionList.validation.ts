import {Page, expect} from '@playwright/test';
import {IValidation, validationRecord} from "../../interfaces/validation.interface";

export class OptionListValidation implements IValidation {
    async validate(page: Page, validation: string, fieldName: string, data: validationRecord): Promise<void> {
        const locator = page.locator(`input[type="${data.elementType}"][name="${fieldName}"]`,);
        await page.locator(`fieldset:has-text("${fieldName}") >> label:has-text("${data.options}")`).click();
        const count = await locator.count();
        const actual: string[] = [];

        for (let i = 0; i < count; i++) {
            const value = await locator.nth(i).getAttribute('value');
            if (value) actual.push(value);
        }
        const expected = data.options;
        expect(actual).toEqual(expected);
    }
}
