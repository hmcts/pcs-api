import {Page} from '@playwright/test';
import {IAction} from '@utils/interfaces/action.interface';
import {performAction} from '@utils/controller';
import {home} from "@data/page-data/home.page.data";
import {SHORT_TIMEOUT} from "../../../playwright.config";


export class signOutAction implements IAction {
  async execute(page: Page, action: string): Promise<void> {
    await performAction('clickButton', home.signOutButton);
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('body', { timeout: SHORT_TIMEOUT });
  }
}
