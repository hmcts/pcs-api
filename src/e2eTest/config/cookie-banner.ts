import type { Page } from '@playwright/test';
import { MEDIUM_TIMEOUT, SHORT_TIMEOUT } from '../playwright.config';

export type CookieBannerType = 'additional' | 'analytics' | 'hide-success';

// The banner is optional, so the wait is the cost of finding out it is absent.
const BANNER_PRESENCE_TIMEOUT = SHORT_TIMEOUT;

// Callers ask for 'additional' then 'analytics' at every site, but XUI renders only one of the
// two. Measured: the 'additional' probe times out 8 times a run and 'analytics' never does, so
// 40s a run went on waiting for a banner this XUI build does not have.
//
// Wait once for whichever banner is actually present and return early if it is not the one asked
// for. Promise.any rather than race, so one probe failing does not cancel the other.
async function bannerPresent(page: Page): Promise<CookieBannerType | null> {
  const additional = page.locator('#accept-additional-cookies');
  const analytics = page
    .getByRole('region', { name: /Cookies on this service/i })
    .getByRole('button', { name: /Accept analytics cookies/i });
  return Promise.any([
    additional.waitFor({ state: 'visible', timeout: BANNER_PRESENCE_TIMEOUT })
      .then((): CookieBannerType => 'additional'),
    analytics.waitFor({ state: 'visible', timeout: BANNER_PRESENCE_TIMEOUT })
      .then((): CookieBannerType => 'analytics'),
  ]).catch(() => null);
}

export async function dismissCookieBanner(page: Page, type: CookieBannerType): Promise<void> {
  try {
    if (type === 'additional' || type === 'analytics') {
      const present = await bannerPresent(page);
      if (present !== type) {
        return;
      }
    }
    if (type === 'additional') {
      const accept = page.locator('#accept-additional-cookies');
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
      await btn.scrollIntoViewIfNeeded();
      await btn.click({ timeout: MEDIUM_TIMEOUT, force: true });
      return;
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : err;
    console.warn(`[cookie-banner] Cookie banner (${type}) could not be dismissed — continuing anyway:`, msg);
  }
}
