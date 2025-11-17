import { Page } from '@playwright/test';
import { SHORT_TIMEOUT, VERY_SHORT_TIMEOUT, MEDIUM_TIMEOUT } from '../playwright.config';

export async function handlePostLoginCookieBanner(page: Page): Promise<void> {
  try {
    await page.waitForLoadState('networkidle', { timeout: MEDIUM_TIMEOUT }).catch(() => {});

    const banner = page.locator('xuilib-cookie-banner').first();
    const isVisible = await banner.isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);

    if (!isVisible) {
      return;
    }

    let acceptButton = banner.getByRole('button', { name: /Accept analytics cookies/i });
    if (!(await acceptButton.isVisible({ timeout: VERY_SHORT_TIMEOUT }).catch(() => false))) {
      acceptButton = banner.locator('button:has-text("Accept analytics cookies")');
      if (!(await acceptButton.isVisible({ timeout: VERY_SHORT_TIMEOUT }).catch(() => false))) {
        return;
      }
    }

    await page.locator('.spinner-container, .loading, [class*="spinner"]')
      .waitFor({ state: 'detached', timeout: SHORT_TIMEOUT })
      .catch(() => {});
    
    await acceptButton.click({ timeout: SHORT_TIMEOUT }).catch(async () => {
      await acceptButton.click({ force: true, timeout: SHORT_TIMEOUT }).catch(() => {});
    });

    await banner.waitFor({ state: 'detached', timeout: SHORT_TIMEOUT }).catch(() => {});
  } catch {
    // Non-blocking - continue even if cookie banner handling fails
  }
}

