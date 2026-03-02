import { IAction } from '@utils/interfaces/action.interface';
import { Page, test } from '@playwright/test';
import {LONG_TIMEOUT} from '../../../playwright.config';

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { timeout: LONG_TIMEOUT });
    });
  }
}
