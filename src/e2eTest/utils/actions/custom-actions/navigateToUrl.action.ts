import { IAction } from '@utils/interfaces/action.interface';
import { Page, test } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from '../../../playwright.config';

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      // ExUI AAT can take more than 30 s to respond after a morning pod rollover; nightly 632
      // on 10 September 2026 failed the caseFlag access management test at 07:43 UTC for this
      // reason. Use VERY_LONG_TIMEOUT (60 s) to accommodate the rollover window.
      await page.goto(url, { timeout: VERY_LONG_TIMEOUT });
    });
  }
}
