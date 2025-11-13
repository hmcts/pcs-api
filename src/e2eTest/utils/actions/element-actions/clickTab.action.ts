import { Page } from '@playwright/test';
import { IAction } from '@utils/interfaces';
import { LONG_TIMEOUT } from '../../../playwright.config';

export class ClickTabAction implements IAction {
  async execute(page: Page, action: string, tabName: string): Promise<void> {
    // Wait for page to be fully loaded
    await page.waitForLoadState('networkidle', { timeout: LONG_TIMEOUT }).catch(() => {
      // Fallback if networkidle takes too long
      return page.waitForLoadState('load', { timeout: LONG_TIMEOUT });
    });

    // Wait for tab container to be present (Angular Material tabs)
    const tabContainer = page.locator('.mat-tab-group, .mat-tab-header, [role="tablist"]').first();
    await tabContainer.waitFor({ state: 'attached', timeout: LONG_TIMEOUT }).catch(() => {
      // If no Material tabs container, wait for any tab structure
      return page.locator('div.mat-tab-label, a[role="tab"]').first().waitFor({ state: 'attached', timeout: LONG_TIMEOUT });
    });

    // Wait for any loading spinners to disappear
    await page.locator('.spinner-container, .loading, [class*="spinner"]').waitFor({ state: 'detached', timeout: LONG_TIMEOUT }).catch(() => {
      // No spinner, continue
    });

    const locator = page.locator(`div.mat-tab-label .mat-tab-label-content:has-text("${tabName}"),
                                  a:text-is("${tabName}")`);

    // Wait for tab to be visible with longer timeout and retry logic
    await locator.waitFor({
      state: 'visible',
      timeout: LONG_TIMEOUT
    });

    // Ensure tab is stable (not animating)
    await page.waitForTimeout(500);

    // Scroll into view if needed
    await locator.scrollIntoViewIfNeeded({ timeout: LONG_TIMEOUT });

    // Wait a bit more after scrolling
    await page.waitForTimeout(300);

    // Click the tab
    await locator.click({ timeout: LONG_TIMEOUT });
  }
}
