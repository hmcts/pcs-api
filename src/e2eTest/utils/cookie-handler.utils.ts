import { Page, Cookie } from '@playwright/test';
import { signInOrCreateAnAccount } from '@data/page-data';
import * as fs from 'fs';

/**
 * Handles cookie consent banners and analytics cookies.
 * UI banner handling is service-specific (not in common repos).
 * Analytics cookie addition follows the pattern from tcoe-playwright-example.
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

  /**
   * Adds analytics cookie to storage state file (following tcoe-playwright-example pattern)
   * This ensures the analytics cookie preference is persisted in the saved session
   * @param sessionPath - Path to the storage state file
   */
  static async addManageCasesAnalyticsCookie(sessionPath: string): Promise<void> {
    try {
      const state = JSON.parse(fs.readFileSync(sessionPath, 'utf-8'));
      const cookies = Array.isArray(state?.cookies) ? state.cookies : [];

      // Get userId from existing cookies (required for cookie name)
      const userId = cookies.find(
        (cookie: Cookie) => cookie.name === '__userid__'
      )?.value;

      if (!userId) {
        console.log('User ID not found in cookies, skipping analytics cookie addition');
        return;
      }

      // Check if analytics cookie already exists
      const analyticsCookieName = `hmcts-exui-cookies-${userId}-mc-accepted`;
      const existingCookie = cookies.find(
        (cookie: Cookie) => cookie.name === analyticsCookieName
      );

      if (!existingCookie) {
        // Get domain from first cookie or derive from session
        const domain = cookies.length > 0 && cookies[0].domain
          ? cookies[0].domain
          : this.resolveHostname(process.env.MANAGE_CASE_BASE_URL || '');

        cookies.push({
          name: analyticsCookieName,
          value: 'true',
          domain,
          path: '/',
          expires: -1,
          httpOnly: false,
          secure: false,
          sameSite: 'Lax',
        });

        state.cookies = cookies;
        fs.writeFileSync(sessionPath, JSON.stringify(state, null, 2), 'utf-8');
        console.log('Analytics cookie added to storage state');
      }
    } catch (error) {
      // Non-blocking - log but don't fail
      console.log(`Failed to add analytics cookie to storage state: ${error instanceof Error ? error.message : String(error)}`);
    }
  }

  /**
   * Resolves hostname from URL (helper method)
   * @param url - URL string
   * @returns hostname
   */
  private static resolveHostname(url: string): string {
    try {
      return new URL(url).hostname;
    } catch {
      try {
        return new URL(`https://${url}`).hostname;
      } catch (fallbackError) {
        throw new Error(
          `Failed to resolve hostname from URL "${url}": ${
            fallbackError instanceof Error ? fallbackError.message : String(fallbackError)
          }`
        );
      }
    }
  }
}

