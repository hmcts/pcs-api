import { chromium, FullConfig } from '@playwright/test';
import { IdamUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';
import { SessionManager } from '@utils/session-manager';
import { user } from '@data/user-data';
import { signInOrCreateAnAccount } from '@data/page-data';

async function globalSetupConfig(config: FullConfig): Promise<void> {
  const baseURL = config.projects[0].use?.baseURL || process.env.MANAGE_CASE_BASE_URL || '';
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    const userEmail = user.claimantSolicitor.email;
    const userPassword = user.claimantSolicitor.password;

    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }

    // Check if session is already valid using SessionManager (which uses SessionUtils from @hmcts/playwright-common)
    if (SessionManager.isSessionValid()) {
      console.log('Valid session found, skipping login...');
      // Storage state is already saved and will be used by Playwright config
      await browser.close();
      return;
    }

    console.log('Performing login and cookie consent...');

    // Navigate to login page
    await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: 30000 });

    // Wait for page to be ready
    try {
      await page.waitForLoadState('domcontentloaded', { timeout: 10000 });
    } catch {
      console.log('Initial page load wait timed out, continuing...');
    }

    // Handle additional cookies consent if present (non-blocking - continue even if it fails)
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

    // Perform login
    await page.waitForSelector('h1:has-text("Sign in or create an account")', { timeout: 10000 });
    await page.getByLabel('Email address').fill(userEmail);
    await page.getByLabel('Password').fill(userPassword);
    await page.getByRole('button', { name: 'Sign in' }).click();

    // Wait for navigation after login
    await page.waitForURL('**/cases', { timeout: 30000 }).catch(async () => {
      // If URL doesn't match, wait for any navigation away from login
      await page.waitForFunction(
        () => !window.location.href.includes('/login') && !window.location.href.includes('/sign-in'),
        { timeout: 30000 }
      ).catch(() => page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => null));
    });

    // Handle analytics cookies consent if present (non-blocking - continue even if it fails)
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

    // Navigate to base URL to ensure we're at the authenticated home page
    await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: 30000 });

    // Wait for page to be ready (use domcontentloaded instead of networkidle for reliability)
    try {
      await page.waitForLoadState('domcontentloaded', { timeout: 10000 });
    } catch {
      console.log('Page load state wait timed out, continuing anyway...');
    }

    // Wait a bit to ensure all cookies are set and page is fully loaded
    await page.waitForTimeout(2000);

    // Save storage state (Playwright's native format - includes cookies and localStorage)
    await SessionManager.saveStorageState(page);
    console.log('Login successful and session saved!');

  } catch (error) {
    console.error('Failed to setup authentication:', error);
    throw error;
  } finally {
    await browser.close();
  }
}

export const getAccessToken = async (): Promise<void> => {
  process.env.IDAM_WEB_URL = accessTokenApiData.idamUrl;
  process.env.IDAM_TESTING_SUPPORT_URL = accessTokenApiData.idamTestingSupportUrl;
  process.env.CREATE_USER_BEARER_TOKEN = await new IdamUtils().generateIdamToken({
    grantType: 'client_credentials',
    clientId: 'pcs-api',
    clientSecret: process.env.PCS_API_IDAM_SECRET as string,
    scope: 'profile roles'
  });
};

export default globalSetupConfig;
