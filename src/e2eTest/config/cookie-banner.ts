import type { Page } from '@playwright/test';
import { MEDIUM_TIMEOUT, SHORT_TIMEOUT } from '../playwright.config';

export type CookieBannerType = 'additional' | 'analytics' | 'hide-success';

// The banner is optional — often already dismissed for the session — so the initial
// wait is the cost of finding out it is absent. At MEDIUM_TIMEOUT that was 10s a miss,
// and one @regression run logged 8 misses: 80s of pure waiting. SHORT_TIMEOUT still
// leaves 5s for a real render, which is well clear of the sub-second appearance seen
// when the banner is present. Only the presence check is shortened; once found, the
// scroll and click keep the longer budget.
const BANNER_PRESENCE_TIMEOUT = SHORT_TIMEOUT;

export async function dismissCookieBanner(page: Page, type: CookieBannerType): Promise<void> {
  try {
    if (type === 'additional') {
      const accept = page.locator('#accept-additional-cookies');
      await accept.waitFor({ state: 'visible', timeout: BANNER_PRESENCE_TIMEOUT });
      await accept.scrollIntoViewIfNeeded();
      await accept.click({ timeout: MEDIUM_TIMEOUT, force: true });
      const hide = page.locator('#hide-message');
      if (await hide.isVisible({ timeout: MEDIUM_TIMEOUT }).catch(() => false)) {
        await hide.scrollIntoViewIfNeeded();
        await hide.click({ timeout: MEDIUM_TIMEOUT, force: true });
      }
      return;
    }
    if (type === 'analytics') {
      const btn = page.getByRole('region', { name: /Cookies on this service/i }).getByRole('button', { name: /Accept analytics cookies/i });
      await btn.waitFor({ state: 'visible', timeout: BANNER_PRESENCE_TIMEOUT });
      await btn.scrollIntoViewIfNeeded();
      await btn.click({ timeout: MEDIUM_TIMEOUT, force: true });
      return;
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : err;
    console.warn(`[cookie-banner] Cookie banner (${type}) could not be dismissed — continuing anyway:`, msg);
  }
}
