import { test as base, BrowserContext, Page } from '@playwright/test';
import { user } from '@data/user-data/permanent.user.data';

type AuthenticatedContextFixtures = {
  authenticatedContext: BrowserContext;
  authenticatedPage: Page;
};

/**
 * Creates a fresh, isolated browser context per test with authenticated session.
 * Eliminates shared storage state by performing programmatic login for each test.
 */
export const test = base.extend<AuthenticatedContextFixtures>({
  authenticatedContext: async ({ browser }, use) => {
    const context = await browser.newContext();
    
    try {
      const userEmail = user.claimantSolicitor.email;
      const userPassword = process.env.IDAM_PCS_USER_PASSWORD || user.claimantSolicitor.password;
      const baseUrl = process.env.MANAGE_CASE_BASE_URL || '';

      if (!userEmail || !userPassword) {
        throw new Error('Missing credentials. Ensure IDAM_PCS_USER_PASSWORD is set.');
      }

      const page = await context.newPage();
      
      try {
        await page.goto(baseUrl, { waitUntil: 'domcontentloaded', timeout: 30000 });
        
        // Wait for and complete login form
        const emailSelector = 'input[type="email"], input[name*="email"], input[id*="email"], input[name="username"]';
        await page.waitForSelector(emailSelector, { timeout: 10000 });
        
        await page.fill(emailSelector, userEmail);
        await page.fill('input[type="password"]', userPassword);
        await page.click('button[type="submit"], input[type="submit"], button:has-text("Sign in"), button:has-text("Sign In")');
        
        // Wait for OAuth callback to complete
        await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {});
      } finally {
        await page.close();
      }
      
      await use(context);
    } finally {
      await context.close();
    }
  },

  authenticatedPage: async ({ authenticatedContext }, use) => {
    const page = await authenticatedContext.newPage();
    await use(page);
    await page.close();
  }
});

export { expect } from '@playwright/test';
