import {expect, Page} from '@playwright/test';
import {IAction} from '@utils/interfaces/action.interface';
import {performAction} from '@utils/controller';
import {home} from '@data/page-data/home.page.data';
import {MEDIUM_TIMEOUT} from '../../../playwright.config';

export class signOutAction implements IAction {
  async execute(page: Page, action: string): Promise<void> {
    await performAction('clickButton', home.signOutButton);
    await expect(async () => {
      const signOut = page.getByText(home.signOutButton);
      if (await signOut.isVisible().catch(() => false)) {
        await performAction('clickButton', home.signOutButton);
      }

      await expect(page.locator('input#username')).toBeVisible();
    }).toPass({
      timeout: MEDIUM_TIMEOUT,
    });

  }
}
