import { expect, Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { performAction } from '@utils/controller';
import { SHORT_TIMEOUT, LONG_TIMEOUT } from 'playwright.config';

export class RetryOnCallBackError implements IAction {
  async execute(page: Page, action: string, button: string, nextPageElement: string,): Promise<void> {
    await expect(async () => {
      await performAction('clickButton', button);
      await expect(page.locator(`h3.error-summary-heading:text-is("The event could not be created"),
                                    h3.error-summary-heading:text-is("Errors"),
                                    h2#error-summary-title:text-is("There is a problem"),
                                    h3#edit-case-event_error-summary-heading
                                    `), `This checks for Unexpected callback errors or server failures. The action retries based on the timeout provided.`).toHaveCount(0, { timeout: SHORT_TIMEOUT });

      await expect(page.locator(`//h1[text()="${nextPageElement}"]`), `If the ${nextPageElement} page is not loaded on the initial attempt,then this retry logic will be activated =>`).toBeVisible({ timeout: SHORT_TIMEOUT });
    }).toPass({
      // One attempt costs the click plus both SHORT_TIMEOUT waits, so ~13s worst case.
      // The old 20s budget therefore allowed one attempt and a truncated second, meaning a
      // callback error needing two real retries could never clear — the point of the retry.
      // LONG_TIMEOUT x3 leaves room for three full attempts.
      timeout: LONG_TIMEOUT * 3,
    });
  }
}