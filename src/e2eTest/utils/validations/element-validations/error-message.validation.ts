import {Page, expect} from '@playwright/test';
import {IValidation, validationRecord} from '../../interfaces/validation.interface';
import {MEDIUM_TIMEOUT} from '../../../playwright.config';

/**
 * Bounded rather than inheriting the 30s global `expect` default: ten negative-path loops wrap a
 * click plus this validation in a 60s `toPass`, and a click alone can spend the 40s actionTimeout,
 * so 30s here made the wrapper unachievable. Not shortened further — two thirds of the 32 call
 * sites invoke this outside a `toPass`, where the timeout is the whole budget.
 */
const ERROR_RENDER_TIMEOUT = MEDIUM_TIMEOUT;

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
    await expect(errorMessage).toBeVisible({ timeout: ERROR_RENDER_TIMEOUT });
  }
}
