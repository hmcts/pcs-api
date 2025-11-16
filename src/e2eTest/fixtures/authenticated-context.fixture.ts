import { test as base, BrowserContext, Page } from '@playwright/test';
import { user } from '@data/user-data';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { signInOrCreateAnAccount } from '@data/page-data';

type AuthenticatedContextFixtures = {
  authenticatedContext: BrowserContext;
  authenticatedPage: Page;
};

export const test = base.extend<AuthenticatedContextFixtures>({
  authenticatedContext: async ({ browser }, use) => {
    const context = await browser.newContext();
    
    try {
      const { email: userEmail, password: userPassword } = user.claimantSolicitor;
      const baseUrl = process.env.MANAGE_CASE_BASE_URL;

      if (!userEmail || !userPassword || !baseUrl) {
        throw new Error('Missing required credentials or MANAGE_CASE_BASE_URL environment variable');
      }

      const page = await context.newPage();
      initializeExecutor(page);

      await performAction('navigateToUrl', baseUrl);
      await performValidation('mainHeader', signInOrCreateAnAccount.mainHeader);
      await performAction('inputText', signInOrCreateAnAccount.emailAddressLabel, userEmail);
      await performAction('inputText', signInOrCreateAnAccount.passwordLabel, userPassword);
      await performAction('clickButton', signInOrCreateAnAccount.signInButton);
      await page.waitForLoadState('networkidle').catch(() => {});
      await performAction('handleCookieConsent', {
        accept: signInOrCreateAnAccount.acceptAnalyticsCookiesButton
      });

      await page.close();
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
