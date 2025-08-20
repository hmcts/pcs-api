import {Page, expect} from '@playwright/test';
import {IValidation, validationData} from '../../interfaces/validation.interface';

export class ErrorMessageValidation implements IValidation {
  async validate(page: Page, fieldName: string, headerAndMessage: string | validationData): Promise<void> {
    let errorMessage;
    if (typeof headerAndMessage === 'string') {
      errorMessage = page.locator(`a.validation-error:has-text("${fieldName}")`);
    } else {
      errorMessage = page.locator(`
        h3.error-summary-heading:has-text("${headerAndMessage.header}") + p:has-text("${headerAndMessage.message}"),
        h3.error-summary-heading:has-text("${headerAndMessage.header}") ~ #errors li:has-text("${headerAndMessage.message}")
      `);
    }
    await expect(errorMessage).toBeVisible();
  }
}
