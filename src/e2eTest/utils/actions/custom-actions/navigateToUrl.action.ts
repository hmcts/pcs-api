import { IAction } from '../../interfaces/action.interface';
import {Page, test} from '@playwright/test';

export class NavigateToUrl implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url);
    });
  }
}
