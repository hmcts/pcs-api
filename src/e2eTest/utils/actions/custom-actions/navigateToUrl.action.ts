import { IAction } from '@utils/interfaces';
import { Page, test } from '@playwright/test';
import { LONG_TIMEOUT, SHORT_TIMEOUT } from '../../../playwright.config';
import { LoginAction } from './login.action';

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });

      const currentUrl = page.url();
      const isLoginPage = currentUrl.includes('/login') || currentUrl.includes('/idam') ||
        await page.locator('input[type="email"], input[name="username"], input[id="username"]').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);

      if (isLoginPage) {
        await new LoginAction().execute(page, 'login', 'claimantSolicitor');
        await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });
      }

      await page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT }).catch(() => {
        return page.waitForLoadState('load', { timeout: LONG_TIMEOUT });
      });
    });
  }
}
