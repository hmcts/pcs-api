import { IAction } from '@utils/interfaces';
import { Page, test } from '@playwright/test';
import { LONG_TIMEOUT, SHORT_TIMEOUT } from '../../../playwright.config';
import { LoginAction } from '@utils/actions/custom-actions/login.action';
import { handlePostLoginCookieBanner } from '@utils/cookie.utils';

export class NavigateToUrlAction implements IAction {
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Navigate to Manage Case URL: ${url}`, async () => {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });

      if (await this.isLoginPage(page)) {
        // Perform login with retry logic for CI environments
        let loginAttempts = 0;
        const maxLoginAttempts = 3;
        
        while (loginAttempts < maxLoginAttempts) {
          try {
            await new LoginAction().execute(page, 'login', 'claimantSolicitor');
            
            // Wait for navigation to complete after login
            await page.waitForLoadState('networkidle', { timeout: LONG_TIMEOUT }).catch(() => {
              // Fallback if networkidle times out
              return page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });
            });
            
            // Wait for redirects (important in CI)
            await page.waitForTimeout(1000);
            
            // Verify login was successful
            const stillOnLoginPage = await this.isLoginPage(page);
            
            if (!stillOnLoginPage) {
              // Login successful, break out of retry loop
              break;
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
            // Wait before retry
            await page.waitForTimeout(2000);
          }
        }
        
        await handlePostLoginCookieBanner(page).catch(() => {});
      }
    });
  }

  private async isLoginPage(page: Page): Promise<boolean> {
    // Wait for page to load and any redirects to complete (critical for CI)
    await page.waitForLoadState('domcontentloaded', { timeout: SHORT_TIMEOUT }).catch(() => {});
    
    // Wait a bit for redirects (especially important in CI environments)
    await page.waitForTimeout(1000);
    
    const url = page.url().toLowerCase();
    
    // Check URL first (more reliable than DOM elements)
    if (url.includes('/login') || url.includes('/idam') || url.includes('/auth') || url.includes('/sign-in')) {
      return true;
    }
    
    // Check for login form elements as secondary check
    const hasUsernameField = await page.locator('#username').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);
    const hasPasswordField = await page.locator('#password').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);
    
    // Only consider it a login page if both username and password fields are visible
    // This reduces false positives where username field might exist on other pages
    return hasUsernameField && hasPasswordField;
  }
}
