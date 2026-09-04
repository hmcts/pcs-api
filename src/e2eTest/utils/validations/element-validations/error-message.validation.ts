import {Page, expect} from '@playwright/test';
import {IValidation, validationRecord} from '../../interfaces/validation.interface';
import {MEDIUM_TIMEOUT} from '../../../playwright.config';

/**
 * Bounded rather than inheriting the 30s global `expect` default.
 *
 * Ten negative-path validation loops in caseManagement.action.ts and enforcement.action.ts
 * wrap `clickButton` + this validation in `toPass({ timeout: VERY_LONG_TIMEOUT })` — 60s.
 * A click can itself consume the 40s `actionTimeout`, so at 30s here the contents could
 * need 70s inside a 60s wrapper: the retry got one attempt and was often killed mid-way,
 * which is what produced "Timeout 60000ms exceeded while waiting on the predicate" over a
 * `locator.fill: Timeout 40000ms exceeded` on createCase.spec.ts:1043.
 *
 * 10s keeps the arithmetic honest (40s click + 10s here fits inside 60s) while staying
 * generous for what is actually being waited on: callers reach here only after
 * `clickButton` has awaited load state and spinner detach, and a validation error is
 * rendered by the response that click produced. Failing fast is what lets the wrapper
 * retry — the same correction measured on signOut in #2595 (flaky 7 → 4).
 *
 * Deliberately not shortened further. Two thirds of the 32 call sites invoke this
 * directly rather than inside a `toPass`, and for those the timeout is the whole budget,
 * not one attempt of several.
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
