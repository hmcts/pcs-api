import {Page, expect} from '@playwright/test';
import {IValidation, validationData} from "../../interfaces/validation.interface";

export class CheckedValidation implements IValidation {
    async validate(page: Page, fieldName: string, data: validationData): Promise<void> {
        const locator = page.locator(`[data-testid="${fieldName}"]`);

        if ('checked' in data) {
            if (data.checked) {
                await expect(locator).toBeChecked();
            } else {
                await expect(locator).not.toBeChecked();
            }
        } else {
            throw new Error('CheckedValidation requires "checked" property in data');
        }
    }
}
