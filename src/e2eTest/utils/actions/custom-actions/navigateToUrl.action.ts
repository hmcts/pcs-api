import { IAction } from '@utils/interfaces';
import { Page, test } from '@playwright/test';
import { LONG_TIMEOUT, SHORT_TIMEOUT } from '../../../playwright.config';
import { LoginAction } from '@utils/actions/custom-actions/login.action';
import { handlePostLoginCookieBanner } from '@utils/cookie.utils';

const isCI = !!process.env.CI;

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });

      if (await this.isLoginPage(page)) {
        // Perform login with retry logic for CI environments
        let loginAttempts = 0;
        const maxLoginAttempts = isCI ? 3 : 1; // Only retry in CI
        
        while (loginAttempts < maxLoginAttempts) {
          try {
            await new LoginAction().execute(page, 'login', 'claimantSolicitor');
            
            // Wait for navigation to complete after login
            // Use networkidle in CI, domcontentloaded locally (faster)
            if (isCI) {
              await page.waitForLoadState('networkidle', { timeout: LONG_TIMEOUT }).catch(() => {
                // Fallback if networkidle times out
                return page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });
              });
              // Wait for redirects (important in CI)
              await page.waitForTimeout(1000);
            } else {
              // Faster for local - just wait for domcontentloaded
              await page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });
            }
            
            // Verify login was successful (only in CI to avoid false positives locally)
            if (isCI) {
              const stillOnLoginPage = await this.isLoginPage(page);
              
              if (!stillOnLoginPage) {
                // Login successful, break out of retry loop
                break;
              }
              
              loginAttempts++;
              if (loginAttempts >= maxLoginAttempts) {
                throw new Error(`Login failed after ${maxLoginAttempts} attempts. Still on login page.`);
              }
            } else {
              // Local: assume success after first attempt
              break;
            }
          } catch (error) {
            loginAttempts++;
            if (loginAttempts >= maxLoginAttempts) {
              throw new Error(`Login failed after ${maxLoginAttempts} attempts: ${error}`);
            }
            // Wait before retry (only in CI)
            if (isCI) {
              await page.waitForTimeout(2000);
            }
          }
        }
        
        await handlePostLoginCookieBanner(page).catch(() => {});
      }
    });
  }

  private async isLoginPage(page: Page): Promise<boolean> {
    // Wait for page to load
    await page.waitForLoadState('domcontentloaded', { timeout: SHORT_TIMEOUT }).catch(() => {});
    
    // Wait for redirects (only in CI - faster locally)
    if (isCI) {
      await page.waitForTimeout(1000);
    }
    
    const url = page.url().toLowerCase();
    
    // Check URL first (more reliable than DOM elements)
    if (url.includes('/login') || url.includes('/idam') || url.includes('/auth') || url.includes('/sign-in')) {
      return true;
    }
    
    // Check for login form elements as secondary check
    const hasUsernameField = await page.locator('#username').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);
    
    // In CI, require both fields to reduce false positives
    // Locally, just check username field for speed
    if (isCI) {
      const hasPasswordField = await page.locator('#password').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);
      return hasUsernameField && hasPasswordField;
    } else {
      return hasUsernameField;
    }
  }
}
