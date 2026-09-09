import { Page, expect } from '@playwright/test';
import { IValidation} from '../../interfaces/validation.interface';
import { pageHeading, readPageHeading } from '@utils/common/locator.utils';

export class MainHeaderValidation implements IValidation {
  /**
   * Reports the heading actually on screen when the expected one never arrives. `pageHeading`
   * filters visible headings *by* `fieldName`, so on a mismatch the locator matches nothing and
   * the failure reads `element(s) not found` — every wrong-page failure looked like a missing
   * element.
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
