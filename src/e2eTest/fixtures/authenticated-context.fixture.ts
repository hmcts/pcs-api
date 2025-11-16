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
    let setupError: Error | null = null;

    try {
      const { email: userEmail, password: userPassword } = user.claimantSolicitor;
      const baseUrl = process.env.MANAGE_CASE_BASE_URL;

      if (!userEmail || !userPassword || !baseUrl) {
        setupError = new Error('Missing required credentials or MANAGE_CASE_BASE_URL environment variable');
      } else {
        const page = await context.newPage();
        try {
          initializeExecutor(page);

          await performAction('navigateToUrl', baseUrl);
          await performAction('handleCookieConsent', {
            accept: signInOrCreateAnAccount.acceptAdditionalCookiesButton,
            hide: signInOrCreateAnAccount.hideThisCookieMessageButton
          });
          await performValidation('mainHeader', signInOrCreateAnAccount.mainHeader);
          await performAction('inputText', signInOrCreateAnAccount.emailAddressLabel, userEmail);
          await performAction('inputText', signInOrCreateAnAccount.passwordLabel, userPassword);
          await performAction('clickButton', signInOrCreateAnAccount.signInButton);
          await page.waitForLoadState('networkidle').catch(() => {});
          await performAction('handleCookieConsent', {
            accept: signInOrCreateAnAccount.acceptAnalyticsCookiesButton
          });
        } catch (error) {
          setupError = error instanceof Error ? error : new Error(String(error));
        } finally {
          await page.close();
        }
      }

      await use(context);

      if (setupError) {
        throw setupError;
      }
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
