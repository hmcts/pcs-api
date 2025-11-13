import { IAction } from '@utils/interfaces';
import { Page, test } from '@playwright/test';
import { LONG_TIMEOUT } from '../../../playwright.config';

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });
      await page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT }).catch(() => {
        return page.waitForLoadState('load', { timeout: LONG_TIMEOUT });
      });
    });
  }
}
