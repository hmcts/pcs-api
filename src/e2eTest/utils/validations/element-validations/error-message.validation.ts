import {Page, expect} from '@playwright/test';
import {IValidation} from '../../interfaces/validation.interface';

export class ErrorMessageValidation implements IValidation {
    async validate(page: Page, fieldName: string): Promise<void> {
        const errorMessage = page.locator(`a.validation-error:has-text("${fieldName}")`);
        await expect(errorMessage).toBeVisible();
    }
}

