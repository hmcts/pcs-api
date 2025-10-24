import { IAction } from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {performAction} from '@utils/controller';
import {signInOrCreateAnAccount} from '@data/page-data/signInOrCreateAnAccount.page.data';

export class signOutAction implements IAction {
  async execute(page: Page, action: string): Promise<void> {
    await performAction('clickButton', signInOrCreateAnAccount.signOutButton);
    await page.locator(`h1:has-text("${signInOrCreateAnAccount.mainHeader}")`).waitFor({ state: 'visible' });
  }
}
