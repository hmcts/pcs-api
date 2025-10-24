import { IAction } from '../../interfaces/action.interface';
import {Page, test} from '@playwright/test';
import {SHORT_TIMEOUT} from '../../../playwright.config';

export class NavigateToUrl implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('body', { timeout: SHORT_TIMEOUT });
    });
  }
}
