import {Page, expect} from '@playwright/test';
import {IValidation, validationData} from '../../interfaces/validation.interface';

export class ErrorMessageValidation implements IValidation {
  async validate(page: Page, fieldName: string, input: string | validationData): Promise<void> {
    let errorMessage;
    if (typeof input === 'string') {
      errorMessage = page.locator(`a.validation-error:has-text("${fieldName}")`);
    } else {
      errorMessage = page.locator(`
        h3.error-summary-heading:has-text("${input.header}") + p:has-text("${input.message}"),
        h3.error-summary-heading:has-text("${input.header}") ~ #errors li:has-text("${input.message}")
      `);
    }
    await expect(errorMessage).toBeVisible();
  }
}
