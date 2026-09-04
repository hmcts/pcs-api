import {expect, Page} from '@playwright/test';
import {IAction} from '@utils/interfaces/action.interface';
import {performAction} from '@utils/controller';
import {home} from '@data/page-data/home.page.data';
import {LONG_TIMEOUT, SHORT_TIMEOUT} from '../../../playwright.config';

export class signOutAction implements IAction {
  async execute(page: Page, action: string): Promise<void> {
    await performAction('clickButton', home.signOutButton);
    await expect(async () => {
      const signOut = page.getByText(home.signOutButton);
      if (await signOut.isVisible().catch(() => false)) {
        await performAction('clickButton', home.signOutButton);
      }

      // Bounded per attempt. Without an explicit timeout this inherits the global expect
      // default of 30s (playwright.config.ts), which is longer than the whole toPass
      // budget below — so a slow sign-out was cut off part-way through its first attempt
      // and never got the retry the wrapper exists to provide.
      await expect(page.locator('input#email')).toBeVisible({ timeout: SHORT_TIMEOUT });
    }).toPass({
      timeout: LONG_TIMEOUT,
    });

  }
}
