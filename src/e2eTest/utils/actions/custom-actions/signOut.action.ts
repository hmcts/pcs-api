import {expect, Page} from '@playwright/test';
import {IAction} from '@utils/interfaces/action.interface';
import {performAction} from '@utils/controller';
import {home} from '@data/page-data/home.page.data';
import {LONG_TIMEOUT, SHORT_TIMEOUT} from '../../../playwright.config';

export class signOutAction implements IAction {
  async execute(page: Page, action: string): Promise<void> {
    await performAction('clickButton', home.signOutButton);
    await expect(async () => {
      const signOut = page.getByText(home.signOutButton).first();
      if (await signOut.isVisible().catch(() => false)) {
        await performAction('clickButton', home.signOutButton);
      }

      // Bounded per attempt: the 30s global default exceeds the toPass budget below.
      await expect(page.locator('input#email')).toBeVisible({ timeout: SHORT_TIMEOUT });
    }).toPass({
      timeout: LONG_TIMEOUT,
    });

  }
}
