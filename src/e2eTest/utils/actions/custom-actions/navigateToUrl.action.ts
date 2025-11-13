import { IAction } from '@utils/interfaces';
import { Page, test } from '@playwright/test';
import { LONG_TIMEOUT } from '../../../playwright.config';

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });

      // Wait for page to be ready
      await page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT }).catch(() => {
        return page.waitForLoadState('load', { timeout: LONG_TIMEOUT });
      });

      // Verify we're authenticated - if redirected to login, storage state wasn't loaded
      const currentUrl = page.url();
      if (currentUrl.includes('/login') || currentUrl.includes('/sign-in') || currentUrl.includes('idam-web-public')) {
        throw new Error(
          `Navigation resulted in login page. Storage state may not be loaded. ` +
          `Current URL: ${currentUrl}. ` +
          `Verify that global-setup completed successfully and .auth/storage-state.json exists.`
        );
      }
    });
  }
}
