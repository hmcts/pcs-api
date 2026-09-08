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

/**
 * Returns true if XUI's banner has already been accepted for some user in this context.
 *
 * globalSetup now pre-sets `hmcts-exui-cookies-<userId>-mc-accepted` for every test user, so in
 * the normal case the banner will never render. Without this check that would make things
 * *slower*, not faster: every call would pay the full BANNER_PRESENCE_TIMEOUT discovering an
 * element that is never coming. Reading cookies is local to the browser and effectively free.
 *
 * Matches on the name prefix rather than a specific id because the context may hold cookies for
 * several users and any accepted one means this page will not show the banner.
 */
async function alreadyAccepted(page: Page): Promise<boolean> {
  try {
    const cookies = await page.context().cookies();
    return cookies.some(c => c.name.startsWith('hmcts-exui-cookies-') && c.name.endsWith('-mc-accepted'));
  } catch {
    return false;
  }
}

export async function dismissCookieBanner(page: Page, type: CookieBannerType): Promise<void> {
  // When the accepted-cookie is present, shorten the presence probe rather than skipping it.
  //
  // Skipping outright would be faster but unsafe: the cookie is pre-set by globalSetup using
  // `new URL(baseUrl).hostname` as the domain, and if that does not match what XUI itself writes,
  // the cookie sits in the jar without suppressing anything. We would then skip dismissal while
  // the banner is still on screen — leaving a click-intercepting overlay, which is worse than the
  // waiting this is meant to remove.
  //
  // A 500ms probe keeps that failure visible and self-correcting (the banner still gets clicked)
  // while removing ~90% of the wait on the normal path.
  //
  // Only applies to the XUI banner. 'additional' is served by the IDAM login page, whose cookie
  // name is unknown — that app is not cloned in this workspace — so that path is unchanged.
  const presenceTimeout = type === 'analytics' && await alreadyAccepted(page)
    ? 500
    : BANNER_PRESENCE_TIMEOUT;
  try {
    if (type === 'additional') {
      const accept = page.locator('#accept-additional-cookies');
      await accept.waitFor({ state: 'visible', timeout: presenceTimeout });
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
      await btn.waitFor({ state: 'visible', timeout: presenceTimeout });
      await btn.scrollIntoViewIfNeeded();
      await btn.click({ timeout: MEDIUM_TIMEOUT, force: true });
      return;
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : err;
    console.warn(`[cookie-banner] Cookie banner (${type}) could not be dismissed — continuing anyway:`, msg);
  }
}
