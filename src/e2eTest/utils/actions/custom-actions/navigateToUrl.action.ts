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

      // Wait for page to be ready
      await page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT }).catch(() => {
        return page.waitForLoadState('load', { timeout: LONG_TIMEOUT });
      });

      // Verify we're not on a login page - if we are, the session wasn't loaded properly
      const currentUrl = page.url();
      if (currentUrl.includes('/login') || currentUrl.includes('/sign-in') || currentUrl.includes('idam-web-public')) {
        throw new Error(
          `Navigation resulted in login page instead of authenticated page. ` +
          `Current URL: ${currentUrl}. ` +
          `This indicates the storage state (session) was not loaded properly. ` +
          `Check that global-setup ran successfully and created .auth/storage-state.json`
        );
      }

      // Wait for authenticated page elements (case list or similar)
      await page.waitForURL('**/cases**', { timeout: LONG_TIMEOUT }).catch(() => {
        // If not on /cases, wait for any authenticated page indicator
        return page.waitForFunction(
          () => !window.location.href.includes('/login') && 
                !window.location.href.includes('/sign-in') &&
                !window.location.href.includes('idam-web-public'),
          { timeout: LONG_TIMEOUT }
        );
      });
    });
  }
}
