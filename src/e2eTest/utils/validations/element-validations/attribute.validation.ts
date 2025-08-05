import {Page, expect} from '@playwright/test';
import {IValidation, validationData} from "../../interfaces/validation.interface";

export class AttributeValidation implements IValidation {
    async validate(page: Page, fieldName: string, data: validationData): Promise<void> {
        const locator = page.locator(`[data-testid="${fieldName}"]`);
        await expect(locator).toHaveAttribute(String(data.attribute), String(data.value));
    }
}
