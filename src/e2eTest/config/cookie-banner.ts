import type { Page } from '@playwright/test';
import { MEDIUM_TIMEOUT } from '../playwright.config';

export type CookieBannerType = 'additional' | 'analytics' | 'hide-success';

export async function dismissCookieBanner(page: Page, type: CookieBannerType): Promise<void> {
  try {
    if (type === 'additional') {
      const accept = page.locator('#cm_cookie_notification #cookie-accept-submit');
      await accept.waitFor({ state: 'visible', timeout: MEDIUM_TIMEOUT });
      await accept.scrollIntoViewIfNeeded();
      await accept.click({ timeout: MEDIUM_TIMEOUT, force: true });
      const hide = page.locator('#cookie-accept-all-success-banner-hide');
      if (await hide.isVisible({ timeout: MEDIUM_TIMEOUT }).catch(() => false)) {
        await hide.scrollIntoViewIfNeeded();
        await hide.click({ timeout: MEDIUM_TIMEOUT, force: true });
      }
      return;
    }
    if (type === 'analytics') {
      const btn = page.getByRole('region', { name: /Cookies on this service/i }).getByRole('button', { name: /Accept analytics cookies/i });
      await btn.waitFor({ state: 'visible', timeout: MEDIUM_TIMEOUT });
      await btn.scrollIntoViewIfNeeded();
      await btn.click({ timeout: MEDIUM_TIMEOUT, force: true });
      return;
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : err;
    console.warn(`[cookie-banner] Cookie banner (${type}) could not be dismissed — continuing anyway:`, msg);
  }
}
