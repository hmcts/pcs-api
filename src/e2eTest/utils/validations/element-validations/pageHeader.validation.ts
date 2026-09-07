import { Page, expect } from '@playwright/test';
import { IValidation} from '../../interfaces/validation.interface';
import { pageHeading, readPageHeading } from '@utils/common/locator.utils';

export class MainHeaderValidation implements IValidation {
  /**
   * Reports the heading that is actually on screen when the expected one never arrives.
   *
   * `pageHeading(page, fieldName)` filters visible headings *by* `fieldName`, so on a mismatch
   * the locator matches nothing and `toHaveText` fails with `element(s) not found` — no
   * indication of where the journey actually is. Every wrong-page failure in this suite
   * therefore looks identical to a missing element.
   *
   * That cost several cycles on createCaseWales:604: the symptom read as a broken radio
   * selector through three attempted fixes, and was only solved once the actual heading and
   * error summary were logged — at which point it was obvious the journey had never left the
   * previous page because a required field was empty.
   *
   * The assertion runs first and unchanged, so the happy path costs nothing and a heading that
   * merely arrives late still passes without a spurious warning. The diagnostic only runs once
   * the assertion has genuinely failed, and re-reads the heading unfiltered so it can name a
   * different page as well as an absent one.
   */
  async validate(page: Page, validation: string, fieldName: string): Promise<void> {
    try {
      await expect(pageHeading(page, fieldName)).toHaveText(fieldName);
    } catch (error) {
      const actual = await readPageHeading(page).catch(() => '');
      const errorSummary = await page
        .locator('.error-summary, #error-summary-title, .govuk-error-summary')
        .first()
        .innerText()
        .catch(() => '');
      const summary = errorSummary.replace(/\s+/g, ' ').trim();
      console.warn(
        `[mainHeader] expected "${fieldName}" but page shows "${actual || '<no heading>'}"`
        + (summary ? `; error summary: ${summary}` : '')
      );
      throw error;
    }
  }
}
