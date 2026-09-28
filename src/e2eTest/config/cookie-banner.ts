import type { Page } from '@playwright/test';
import { MEDIUM_TIMEOUT, SHORT_TIMEOUT } from '../playwright.config';

export type CookieBannerType = 'additional' | 'analytics' | 'hide-success';

async function bannerPresent(page: Page): Promise<CookieBannerType | null> {
  const additional = page.locator('#accept-additional-cookies');
  if (await additional.isVisible({timeout: 1000}).catch(() => false)) {
    return 'additional';
  }
  const analytics = page
    .getByRole('region', { name: /Cookies on this service/i })
    .getByRole('button', { name: /Accept analytics cookies/i });

  if (await analytics.isVisible({timeout: 1000}).catch(() => false)) {
    return 'analytics';
  }

  return null;
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
