import {Page, expect} from '@playwright/test';
import {IValidation, validationData} from "../../interfaces/validation.interface";

export class EnabledValidation implements IValidation {
    async validate(page: Page, fieldName: string, data: validationData): Promise<void> {
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
