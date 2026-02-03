import {Page, expect} from '@playwright/test';
import {IValidation, validationRecord} from '../../interfaces/validation.interface';

export class ErrorMessageValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, error: string | validationRecord): Promise<void> {
    let errorMessage;
    if (typeof error === 'string') {
      errorMessage = page.locator(`a.validation-error:text-is("${error}")`);
    } else {
      errorMessage = page.locator(`
        h3.error-summary-heading:text-is("${error.header}") + p:text-is("${error.message}"),
        h3.error-summary-heading:text-is("${error.header}") ~ #errors li:text-is("${error.message}"),
        h2#error-summary-title:text-is("${error.header}") ~ div ul li a:text-is("${error.message}"),
        h3#edit-case-event_error-summary-heading ~ ul li:text-is("${error.message}")
      `);
    }
    await expect(errorMessage).toBeVisible();
  }
}