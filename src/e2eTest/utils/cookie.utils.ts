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
   * Adds Manage Cases analytics cookie to storage state file
   * Following tcoe-playwright-example pattern exactly
   */
  public async addManageCasesAnalyticsCookie(sessionPath: string, baseURL?: string): Promise<void> {
    try {
      const domain = baseURL 
        ? this.resolveHostname(baseURL) 
        : this.resolveHostname(process.env.MANAGE_CASE_BASE_URL || '');
      const state = JSON.parse(fs.readFileSync(sessionPath, 'utf-8'));
      const userId = state.cookies.find(
        (cookie: Cookie) => cookie.name === '__userid__'
      )?.value;
      state.cookies.push({
        name: `hmcts-exui-cookies-${userId}-mc-accepted`,
        value: 'true',
        domain,
        path: '/',
        expires: -1,
        httpOnly: false,
        secure: false,
        sameSite: 'Lax',
      });
      fs.writeFileSync(sessionPath, JSON.stringify(state, null, 2));
    } catch (error) {
      throw new Error(`Failed to read or write session data: ${error}`);
    }
  }
}
