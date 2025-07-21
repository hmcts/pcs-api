// validations/error-message.validation.ts
import { Page, expect } from '@playwright/test';
import { IValidation, ValidationData } from '../../interfaces/validation.interface';

interface ErrorMessageValidationData extends ValidationData {
  header: string;
  errorHasLink: string;
}

export class ErrorMessageValidation implements IValidation {
  async validate(page: Page, _fieldName: string, data: ErrorMessageValidationData): Promise<void> {
    // Validate error summary heading
    const headingLocator = page.locator('h2.govuk-error-summary__title');
    await expect(headingLocator).toHaveText(data.header);

    // Validate link text in the error list
    const linkLocator = page.locator('div.govuk-error-summary__body a.validation-error');
    await expect(linkLocator).toHaveText(data.errorHasLink);
  }
}
