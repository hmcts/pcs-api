import { IAction } from '@utils/interfaces';
import { Page, test } from '@playwright/test';
import { LONG_TIMEOUT, SHORT_TIMEOUT } from '../../../playwright.config';
import { reAuthenticateIfNeeded } from '@utils/session.utils';

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });
      await page.waitForTimeout(SHORT_TIMEOUT);
      await reAuthenticateIfNeeded(page);
    });
  }
}
