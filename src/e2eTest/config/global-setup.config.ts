import { chromium, FullConfig } from '@playwright/test';
import { IdamUtils, IdamPage } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';
import { SessionManager } from '@utils/session-manager';
import { CookieHandler } from '@utils/cookie-handler.utils';
import { user } from '@data/user-data';

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
    await CookieHandler.handleAdditionalCookies(page);

    // Perform login using IdamPage from @hmcts/playwright-common
    const idamPage = new IdamPage(page);
    await idamPage.login({
      username: userEmail,
      password: userPassword,
      sessionFile: SessionManager.getStorageStatePath(),
    });

    // Wait for navigation after login
    await page.waitForURL('**/cases', { timeout: 30000 }).catch(async () => {
      // If URL doesn't match, wait for any navigation away from login
      await page.waitForFunction(
        () => !window.location.href.includes('/login') && !window.location.href.includes('/sign-in'),
        { timeout: 30000 }
      ).catch(() => page.waitForLoadState('domcontentloaded', { timeout: 10000 }).catch(() => null));
    });

    // Handle analytics cookies consent if present (non-blocking - continue even if it fails)
    await CookieHandler.handleAnalyticsCookies(page);

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

    // Add analytics cookie to storage state file for persistence (following tcoe-playwright-example pattern)
    await CookieHandler.addManageCasesAnalyticsCookie(SessionManager.getStorageStatePath());
    
    // Ensure storage state is up to date with all cookies
    await SessionManager.saveStorageState(page);
    
    console.log('Login successful and session saved!');

  } catch (error) {
    console.error('Failed to setup authentication:', error);
    // Re-throw to fail the global setup, which will prevent tests from running
    throw error;
  } finally {
    // Ensure browser is always closed, even if an error occurs
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
