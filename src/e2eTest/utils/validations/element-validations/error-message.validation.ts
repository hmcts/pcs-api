import {Page, expect} from '@playwright/test';
import {IValidation, validationData} from '../../interfaces/validation.interface';

export class ErrorMessageValidation implements IValidation {
  async validate(page: Page, fieldName: string, error: string | validationData): Promise<void> {
    let errorMessage;
    if (typeof error === 'string') {
      errorMessage = page.locator(`a.validation-error:has-text("${error}")`);
    } else {
      errorMessage = page.locator(`
        h3.error-summary-heading:has-text("${error.header}") + p:has-text("${error.message}"),
        h3.error-summary-heading:has-text("${error.header}") ~ #errors li:has-text("${error.message}")
      `);
    }
    await expect(errorMessage).toBeVisible();
  }
}
