import type { Page } from '@playwright/test';
import { MEDIUM_TIMEOUT, SHORT_TIMEOUT } from '../playwright.config';

export type CookieBannerType = 'additional' | 'analytics' | 'hide-success';

/**
 * Temporary diagnostic — DO NOT MERGE.
 *
 * Logs the cookies present after a banner is dismissed, so the names needed to pre-set them can
 * be read from a real preview run rather than guessed.
 *
 * Why guessing is not good enough: XUI's banner keys off
 * `hmcts-exui-cookies-${userId}-mc-accepted` (rpx-xui-webapp app.component.ts:185), so the name
 * embeds the IDAM user id and cannot be pre-set without knowing it. The other banner
 * (`#accept-additional-cookies`) is served by the IDAM login page, which is not cloned in this
 * workspace, so its cookie name is not discoverable from source at all.
 *
 * Dumps name + domain only, never values — these are session cookies.
 */
async function dumpCookies(page: Page, label: string): Promise<void> {
  try {
    const cookies = await page.context().cookies();
    const summary = cookies
      .map(c => `${c.name}@${c.domain}`)
      .sort()
      .join(', ');
    console.log(`[cookie-dump] after ${label} (${cookies.length}): ${summary}`);
  } catch (err) {
    console.log(`[cookie-dump] after ${label}: failed — ${err instanceof Error ? err.message : err}`);
  }
}

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
      await dumpCookies(page, "additional-accept");
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
      await dumpCookies(page, "analytics-accept");
      return;
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : err;
    console.warn(`[cookie-banner] Cookie banner (${type}) could not be dismissed — continuing anyway:`, msg);
  }
}
