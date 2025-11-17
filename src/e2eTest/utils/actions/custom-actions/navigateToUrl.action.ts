import { IAction } from '@utils/interfaces';
import { Page, test } from '@playwright/test';
import { LONG_TIMEOUT, SHORT_TIMEOUT } from '../../../playwright.config';
import { LoginAction } from '@utils/actions/custom-actions/login.action';
import { handlePostLoginCookieBanner } from '@utils/cookie.utils';

const isCI = !!process.env.CI;

/**
 * Checks if the current page is a login page by examining URL and form elements.
 */
async function isLoginPage(page: Page): Promise<boolean> {
  await page.waitForLoadState('domcontentloaded', { timeout: SHORT_TIMEOUT }).catch(() => {});
  
  if (isCI) {
    await page.waitForTimeout(1000);
  }
  
  const url = page.url().toLowerCase();
  if (url.includes('/login') || url.includes('/idam') || url.includes('/auth') || url.includes('/sign-in')) {
    return true;
  }
  
  const hasUsernameField = await page.locator('#username').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);
  
  if (isCI) {
    const hasPasswordField = await page.locator('#password').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);
    return hasUsernameField && hasPasswordField;
  }
  
  return hasUsernameField;
}

/**
 * Action to navigate to a specific URL with automatic login handling if needed.
 * Includes retry logic for CI environments and environment-aware wait strategies.
 */
export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });

      if (await isLoginPage(page)) {
        await this.performLoginWithRetry(page);
        await handlePostLoginCookieBanner(page).catch(() => {});
      }
    });
  }

  /**
   * Performs login with retry logic (CI only) and environment-aware wait strategies.
   */
  private async performLoginWithRetry(page: Page): Promise<void> {
    let loginAttempts = 0;
    const maxLoginAttempts = isCI ? 3 : 1;
    
    while (loginAttempts < maxLoginAttempts) {
      try {
        await new LoginAction().execute(page, 'login', 'claimantSolicitor');
        await this.waitForLoginCompletion(page);
        
        if (!isCI || !(await isLoginPage(page))) {
          return; // Login successful
        }
        
        loginAttempts++;
        if (loginAttempts >= maxLoginAttempts) {
          throw new Error(`Login failed after ${maxLoginAttempts} attempts. Still on login page.`);
        }
      } catch (error) {
        loginAttempts++;
        if (loginAttempts >= maxLoginAttempts) {
          throw new Error(`Login failed after ${maxLoginAttempts} attempts: ${error}`);
        }
        if (isCI) {
          await page.waitForTimeout(2000);
        }
      }
    }
  }

  /**
   * Waits for login to complete using environment-appropriate strategy.
   */
  private async waitForLoginCompletion(page: Page): Promise<void> {
    if (isCI) {
      await page.waitForLoadState('networkidle', { timeout: LONG_TIMEOUT }).catch(() => {
        return page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });
      });
      await page.waitForTimeout(1000);
    } else {
      await page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });
    }
  }
}
