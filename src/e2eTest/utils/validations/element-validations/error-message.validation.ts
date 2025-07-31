import { Page, expect } from '@playwright/test';
import { IValidation, ValidationData } from '../../interfaces/validation.interface';

interface ErrorMessageValidationData extends ValidationData {
  header: string;
  errorHasLink: string;
}

export class ErrorMessageValidation implements IValidation {
  async validate(page: Page, _fieldName: string, data: ErrorMessageValidationData): Promise<void> {

    await expect(page.locator('h2.govuk-error-summary__title')).toHaveText(data.header);

    await expect(page.locator('a.validation-error', { hasText: data.errorHasLink })).toHaveText(data.errorHasLink);
  }
}

