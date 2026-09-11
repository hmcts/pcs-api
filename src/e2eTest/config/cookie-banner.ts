import type { Page } from '@playwright/test';
import { MEDIUM_TIMEOUT, SHORT_TIMEOUT } from '../playwright.config';

export type CookieBannerType = 'additional' | 'analytics' | 'hide-success';

const BANNER_PRESENCE_TIMEOUT = SHORT_TIMEOUT;

// XUI renders only one of the two banners, so probing each in turn pays the timeout for the absent
// one. Promise.any rather than race: one probe failing must not cancel the other.
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
