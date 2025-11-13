import fs from 'fs';
import { Cookie } from '@playwright/test';

/**
 * Cookie utilities following tcoe-playwright-example pattern exactly
 * This matches the implementation from tcoe-playwright-example/playwright-e2e/utils/cookie.utils.ts
 */
export class CookieUtils {
  private resolveHostname(url: string): string {
    try {
      return new URL(url).hostname;
    } catch {
      try {
        return new URL(`https://${url}`).hostname;
      } catch (fallbackError) {
        throw new Error(
          `Failed to resolve hostname from URL "${url}": ${
            fallbackError instanceof Error ? fallbackError.message : fallbackError
          }`
        );
      }
    }
  }

  /**
   * Adds all consent cookies directly to storage state file
   * This bypasses UI interaction and prevents cookie banners from appearing
   * Following tcoe-playwright-example pattern for analytics cookie
   */
  public async addAllConsentCookies(sessionPath: string, baseURL?: string): Promise<void> {
    try {
      const domain = baseURL
        ? this.resolveHostname(baseURL)
        : this.resolveHostname(process.env.MANAGE_CASE_BASE_URL || '');
      const state = JSON.parse(fs.readFileSync(sessionPath, 'utf-8'));
      const cookies = Array.isArray(state?.cookies) ? state.cookies : [];

      // Get userId from existing cookies (required for analytics cookie name)
      const userId = cookies.find(
        (cookie: Cookie) => cookie.name === '__userid__'
      )?.value;

      if (!userId) {
        console.log('User ID not found in cookies, skipping consent cookie addition');
        return;
      }

      // Add analytics cookie (following tcoe-playwright-example pattern)
      const analyticsCookieName = `hmcts-exui-cookies-${userId}-mc-accepted`;
      const existingAnalyticsCookie = cookies.find(
        (cookie: Cookie) => cookie.name === analyticsCookieName
      );

      if (!existingAnalyticsCookie) {
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
        console.log('Analytics cookie added to storage state');
      }

      // Add additional cookies consent cookie
      // Common cookie names for additional cookies consent (update if your app uses different name)
      const additionalCookieNames = [
        'cookieConsent',
        'cookie-preference',
        'cookie_preference',
        'cm_cookie_notification',
        'cookie-notification'
      ];

      let additionalCookieAdded = false;
      for (const cookieName of additionalCookieNames) {
        const existingCookie = cookies.find(
          (cookie: Cookie) => cookie.name === cookieName
        );
        if (!existingCookie) {
          cookies.push({
            name: cookieName,
            value: 'true',
            domain,
            path: '/',
            expires: -1,
            httpOnly: false,
            secure: false,
            sameSite: 'Lax',
          });
          additionalCookieAdded = true;
          console.log(`Additional cookies consent cookie (${cookieName}) added to storage state`);
          break; // Only add one
        }
      }

      if (!additionalCookieAdded) {
        console.log('Additional cookies consent cookie already exists or could not be determined');
      }

      // Save updated cookies back to storage state
      state.cookies = cookies;
      fs.writeFileSync(sessionPath, JSON.stringify(state, null, 2), 'utf-8');
      console.log('All consent cookies added to storage state');
    } catch (error) {
      throw new Error(`Failed to add consent cookies to storage state: ${error}`);
    }
  }
}
