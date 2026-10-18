import { expect, Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { performAction } from '@utils/controller';
import { SHORT_TIMEOUT, MEDIUM_TIMEOUT } from 'playwright.config';

export class RetryOnCallBackError implements IAction {
  async execute(page: Page, action: string, button: string, nextPageElement: string,): Promise<void> {
    const nextPage = page.locator(`//h1[text()="${nextPageElement}"]`);
    let attempt = 0;
    await expect(async () => {
      attempt++;
      // A slow page can arrive after the previous attempt stopped waiting for it. Clicking again would press
      // the next page's button (or wait for one it does not have), so a retry first checks whether it is there.
      if (attempt > 1 && await nextPage.waitFor({ timeout: SHORT_TIMEOUT }).then(() => true, () => false)) {
        return;
      }
      await performAction('clickButton', button);
      const errorSummary = page.locator(`h3.error-summary-heading:text-is("The event could not be created"),
                                    h3.error-summary-heading:text-is("Errors"),
                                    h2#error-summary-title:text-is("There is a problem"),
                                    h3#edit-case-event_error-summary-heading`);
      const errorDetails = page.locator('.govuk-error-summary, .error-summary');
      const errorCount = await errorSummary.count();
      const errorText = errorCount > 0 ? (await errorDetails.allInnerTexts()).join(' | ').replace(/\s+/g, ' ') : '';
      expect(errorCount, `Unexpected callback error or server failure after clicking "${button}" (expected page "${nextPageElement}"). Error shown: ${errorText}`).toBe(0);

      await expect(nextPage, `If the ${nextPageElement} page is not loaded on the initial attempt,then this retry logic will be activated =>`).toBeVisible({ timeout: SHORT_TIMEOUT });
    }).toPass({
      timeout: MEDIUM_TIMEOUT + MEDIUM_TIMEOUT,
    });
  }
}
