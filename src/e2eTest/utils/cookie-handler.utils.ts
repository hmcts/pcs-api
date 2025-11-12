import { Page } from '@playwright/test';
import { signInOrCreateAnAccount } from '@data/page-data';

/**
 * Handles cookie consent banners in a non-blocking way.
 * If cookie banners are not found or interactions fail, the function logs a message and continues.
 * This ensures tests continue even if cookie handling fails.
 */
export class CookieHandler {
  /**
   * Handles additional cookies consent banner if present
   * @param page - Playwright page instance
   */
  static async handleAdditionalCookies(page: Page): Promise<void> {
    try {
      const additionalCookiesBanner = page.locator('#cm_cookie_notification');
      const acceptAdditionalBtn = additionalCookiesBanner.getByRole('button', {
        name: signInOrCreateAnAccount.acceptAdditionalCookiesButton,
      });

      if (await additionalCookiesBanner.isVisible({ timeout: 5000 }).catch(() => false)) {
        await acceptAdditionalBtn.click({ timeout: 5000 }).catch(() => {
          console.log('Failed to click additional cookies accept button, continuing...');
        });
        await additionalCookiesBanner.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => null);

        // Handle hide button if present
        try {
          const successBanner = page.locator('#accept-all-cookies-success');
          const hideBtn = successBanner.getByRole('button', {
            name: signInOrCreateAnAccount.hideThisCookieMessageButton,
          });
          if (await successBanner.isVisible({ timeout: 2000 }).catch(() => false)) {
            await hideBtn.click({ timeout: 5000 }).catch(() => {
              console.log('Failed to click hide button, continuing...');
            });
            await successBanner.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => null);
          }
        } catch {
          // Hide button handling failed - not critical, continue
          console.log('Hide button handling failed, continuing...');
        }
      }
    } catch {
      // Cookie consent handling failed - log but don't fail the setup
      console.log('Additional cookies banner handling failed, continuing with login...');
    }
  }

  /**
   * Handles analytics cookies consent banner if present
   * @param page - Playwright page instance
   */
  static async handleAnalyticsCookies(page: Page): Promise<void> {
    try {
      const analyticsBanner = page.locator('xuilib-cookie-banner');
      const acceptAnalyticsBtn = analyticsBanner.getByRole('button', {
        name: signInOrCreateAnAccount.acceptAnalyticsCookiesButton,
      });

      if (await analyticsBanner.isVisible({ timeout: 5000 }).catch(() => false)) {
        await acceptAnalyticsBtn.click({ timeout: 5000 }).catch(() => {
          console.log('Failed to click analytics cookies accept button, continuing...');
        });
        await analyticsBanner.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => null);
      }
    } catch {
      // Analytics cookie consent handling failed - log but don't fail the setup
      console.log('Analytics cookies banner handling failed, continuing...');
    }
  }

  /**
   * Handles all cookie consent banners (additional and analytics)
   * @param page - Playwright page instance
   */
  static async handleAllCookies(page: Page): Promise<void> {
    await this.handleAdditionalCookies(page);
    await this.handleAnalyticsCookies(page);
  }
}

