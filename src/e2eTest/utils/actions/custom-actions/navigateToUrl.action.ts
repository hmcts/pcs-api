import { IAction } from '@utils/interfaces';
import { Page, test } from '@playwright/test';
import { LONG_TIMEOUT } from '../../../playwright.config';

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      try {
        await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });
      } catch (error) {
        // If domcontentloaded fails, try with load as fallback
        console.log('Navigation with domcontentloaded failed, retrying with load...');
        await page.goto(url, { waitUntil: 'load', timeout: LONG_TIMEOUT });
      }
    });
  }
}
