import {Page, expect} from '@playwright/test';
import {IValidation, validationRecord} from '../../interfaces/validation.interface';

export class ErrorMessageValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, error: string | validationRecord): Promise<void> {
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
