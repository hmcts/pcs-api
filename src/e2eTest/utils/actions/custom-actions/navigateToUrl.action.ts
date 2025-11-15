import { IAction } from '@utils/interfaces';
import { Page, test } from '@playwright/test';
import { LONG_TIMEOUT, SHORT_TIMEOUT } from '../../../playwright.config';
import { handlePostLoginCookieBanner } from '@utils/cookie.utils';
import { performAction } from "@utils/controller";
import { user } from "@data/page-data";

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });

      if (await this.isLoginPage(page)) {
        await performAction('login',user.claimantSolicitor);
        await page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });
        await handlePostLoginCookieBanner(page).catch(() => {});
      }
    });
  }

  private async isLoginPage(page: Page): Promise<boolean> {
    const url = page.url().toLowerCase();
    if (url.includes('/login') || url.includes('/idam') || url.includes('/auth') || url.includes('/sign-in')) {
      return true;
    }
    return await page.locator('#username').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);
  }
}
