import { IdamUtils, ServiceAuthUtils } from '@hmcts/playwright-common';
import { chromium, Browser, BrowserContext, Page } from '@playwright/test';
import { accessTokenApiData, s2STokenApiData } from '@data/api-data';
import { user } from '@data/user-data';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';
import { home } from '@data/page-data';
import * as path from 'path';
import * as fs from 'fs';

// Path relative to the e2eTest directory
const STORAGE_STATE_PATH = path.join(__dirname, '../.auth/storage-state.json');

async function globalSetupConfig(): Promise<void> {
  // Get API tokens (for API-based test setup)
  await getAccessToken();
  await getS2SToken();
  
  // Perform browser-based login and save authentication state
  // This allows all tests to reuse the same authenticated session
  await authenticateAndSaveState();
}

/**
 * Performs login and saves authentication state (cookies, localStorage) to a file
 * This allows all tests to reuse the same authenticated session
 */
async function authenticateAndSaveState(): Promise<string> {
  console.log('🔐 Starting authentication setup...');

  // Validate required environment variables before starting browser
  const baseUrl = process.env.MANAGE_CASE_BASE_URL;
  if (!baseUrl) {
    throw new Error('MANAGE_CASE_BASE_URL environment variable is not set');
  }

  const userEmail = user.claimantSolicitor.email;
  const userPassword = user.claimantSolicitor.password || process.env.IDAM_PCS_USER_PASSWORD;
  if (!userEmail || !userPassword) {
    throw new Error('Login failed: missing credentials. Ensure IDAM_PCS_USER_PASSWORD is set.');
  }

  // Ensure the .auth directory exists
  const authDir = path.dirname(STORAGE_STATE_PATH);
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true });
  }

  const browser: Browser = await chromium.launch({
    headless: !!process.env.CI,
  });

  const context: BrowserContext = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
  });

  const page: Page = await context.newPage();

  try {
    console.log(`📍 Navigating to: ${baseUrl}`);
    await page.goto(baseUrl, { waitUntil: 'domcontentloaded' });
    
    // Wait for page to be ready
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000); // Give time for cookie banners to appear

    // Handle cookie consent (additional cookies) - with retry
    let retryCount = 0;
    const maxRetries = 3;
    while (retryCount < maxRetries) {
      await handleCookieConsent(
        page,
        signInOrCreateAnAccount.acceptAdditionalCookiesButton,
        signInOrCreateAnAccount.hideThisCookieMessageButton
      );
      
      // Check if banner is still visible
      const banner = page.locator('#cm_cookie_notification');
      const isStillVisible = await banner.isVisible({ timeout: 2000 }).catch(() => false);
      
      if (!isStillVisible) {
        console.log(`✅ Additional cookies banner handled successfully`);
        break;
      }
      
      retryCount++;
      if (retryCount < maxRetries) {
        console.log(`🔄 Retrying cookie consent handling (attempt ${retryCount + 1}/${maxRetries})`);
        await page.waitForTimeout(2000);
      }
    }

    // Perform login (userEmail and userPassword are already validated above)
    console.log(`🔑 Logging in as: ${userEmail}`);

    // Wait for login page header
    await page.waitForSelector(`h1:has-text("${signInOrCreateAnAccount.mainHeader}")`, { timeout: 30000 });

    // Fill email using the same locator pattern as inputText action
    const emailLocator = page.getByRole('textbox', { name: signInOrCreateAnAccount.emailAddressLabel, exact: true });
    const emailInput = (await emailLocator.count() > 0)
      ? emailLocator
      : page.locator(`:has-text("${signInOrCreateAnAccount.emailAddressLabel}") ~ input:visible:enabled,
                      label:has-text("${signInOrCreateAnAccount.emailAddressLabel}") ~ textarea,
                      label:has-text("${signInOrCreateAnAccount.emailAddressLabel}") + div input`);
    await emailInput.fill(userEmail);

    // Fill password - password fields typically use input[type="password"]
    const passwordInput = page.locator(`label:has-text("${signInOrCreateAnAccount.passwordLabel}") ~ input[type="password"]:visible:enabled,
                                       :has-text("${signInOrCreateAnAccount.passwordLabel}") ~ input[type="password"]:visible:enabled,
                                       label:has-text("${signInOrCreateAnAccount.passwordLabel}") + div input[type="password"]`);
    await passwordInput.fill(userPassword);

    // Click sign in button using the same pattern as clickButton action
    const signInButton = page.getByRole('button', { name: signInOrCreateAnAccount.signInButton });
    await signInButton.click();

    // Wait for navigation after login (wait for URL to change or a specific element that indicates successful login)
    await page.waitForURL((url) => !url.href.includes('/login') && !url.href.includes('/sign-in'), {
      timeout: 30000,
    }).catch(() => {
      return page.waitForSelector('body', { timeout: 30000 });
    });

    // Handle analytics cookie consent (if present) - with retry
    let analyticsRetryCount = 0;
    const analyticsMaxRetries = 3;
    while (analyticsRetryCount < analyticsMaxRetries) {
      await handleCookieConsent(
        page,
        signInOrCreateAnAccount.acceptAnalyticsCookiesButton
      );
      
      // Check if banner is still visible
      const analyticsBanner = page.locator('xuilib-cookie-banner');
      const isStillVisible = await analyticsBanner.isVisible({ timeout: 2000 }).catch(() => false);
      
      if (!isStillVisible) {
        console.log(`✅ Analytics cookies banner handled successfully`);
        break;
      }
      
      analyticsRetryCount++;
      if (analyticsRetryCount < analyticsMaxRetries) {
        console.log(`🔄 Retrying analytics cookie consent handling (attempt ${analyticsRetryCount + 1}/${analyticsMaxRetries})`);
        await page.waitForTimeout(2000);
      }
    }

    // Navigate to home page to ensure cookies are persisted
    await page.goto(baseUrl, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('domcontentloaded');
    
    // Handle cookie consent again after navigation (in case it appears)
    await handleCookieConsent(
      page,
      signInOrCreateAnAccount.acceptAdditionalCookiesButton,
      signInOrCreateAnAccount.hideThisCookieMessageButton
    );
    await handleCookieConsent(
      page,
      signInOrCreateAnAccount.acceptAnalyticsCookiesButton
    );

    // Wait a bit to ensure all cookies/localStorage are set and persisted
    await page.waitForTimeout(2000);

    // Verify cookies are actually set before saving
    const cookies = await context.cookies();
    console.log(`🍪 Found ${cookies.length} cookies before saving state`);
    
    // Log cookie names for debugging
    if (cookies.length > 0) {
      const cookieNames = cookies.map(c => c.name).join(', ');
      console.log(`🍪 Cookie names: ${cookieNames}`);
    }

    const locator = page.locator(`div.mat-tab-label .mat-tab-label-content:has-text("${home.createCaseTab}"),
                                  a:text-is("${home.createCaseTab}")`);
    await locator.waitFor({ state: 'visible' });
    await locator.click();

    // Wait a bit more after clicking to ensure all state is saved
    await page.waitForTimeout(2000);

    // Navigate once more to ensure cookies persist across navigation
    await page.goto(baseUrl, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);

    // Final check - handle cookie consent one more time if it appears
    await handleCookieConsent(
      page,
      signInOrCreateAnAccount.acceptAdditionalCookiesButton,
      signInOrCreateAnAccount.hideThisCookieMessageButton
    );
    await handleCookieConsent(
      page,
      signInOrCreateAnAccount.acceptAnalyticsCookiesButton
    );

    // Wait for cookies to be fully persisted
    await page.waitForTimeout(2000);

    // Verify cookies again before saving
    const finalCookies = await context.cookies();
    console.log(`🍪 Found ${finalCookies.length} cookies before final save`);

    // Save the authentication state (this includes all cookies and localStorage)
    await context.storageState({ path: STORAGE_STATE_PATH });
    
    // Verify the saved state contains cookies
    const savedState = JSON.parse(fs.readFileSync(STORAGE_STATE_PATH, 'utf-8'));
    const savedCookies = savedState.cookies || [];
    console.log(`✅ Authentication state saved to: ${STORAGE_STATE_PATH}`);
    console.log(`🍪 Saved ${savedCookies.length} cookies in storageState`);
    
    if (savedCookies.length > 0) {
      const savedCookieNames = savedCookies.map((c: any) => c.name).join(', ');
      console.log(`🍪 Saved cookie names: ${savedCookieNames}`);
    }

    return STORAGE_STATE_PATH;
  } catch (error) {
    console.error('❌ Authentication setup failed:', error);
    // Take a screenshot for debugging
    await page.screenshot({ path: path.join(authDir, 'auth-failure.png'), fullPage: true });
    throw error;
  } finally {
    await browser.close();
  }
}

/**
 * Helper function to handle cookie consent banners
 * This is a simplified version without test.step() for use in global setup
 */
async function handleCookieConsent(
  page: Page,
  acceptButtonName: string,
  hideButtonName?: string
): Promise<void> {
  try {
    let consentBanner;
    let acceptBtn;
    let successBanner;
    let hideBtn;

    if (acceptButtonName === 'Accept additional cookies') {
      consentBanner = page.locator('#cm_cookie_notification');
      acceptBtn = consentBanner.getByRole('button', { name: acceptButtonName });
      if (hideButtonName) {
        successBanner = page.locator('#accept-all-cookies-success');
        hideBtn = successBanner.getByRole('button', { name: hideButtonName });
      }
    } else if (acceptButtonName === 'Accept analytics cookies') {
      consentBanner = page.locator('xuilib-cookie-banner');
      acceptBtn = consentBanner.getByRole('button', { name: acceptButtonName });
    } else {
      return;
    }

    const isBannerVisible = await consentBanner.isVisible().catch(() => false);

    if (!isBannerVisible) {
      return;
    }

    await acceptBtn.waitFor({ state: 'visible', timeout: 10000 });
    await page.waitForTimeout(500);
    await acceptBtn.click();

    await consentBanner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {
      return consentBanner.waitFor({ state: 'detached', timeout: 10000 });
    });

    if (successBanner && hideBtn) {
      const isSuccessBannerVisible = await successBanner.isVisible().catch(() => false);

      if (isSuccessBannerVisible) {
        await hideBtn.waitFor({ state: 'visible', timeout: 10000 });
        await page.waitForTimeout(500);
        await hideBtn.click();
        await successBanner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {
          return successBanner.waitFor({ state: 'detached', timeout: 10000 });
        });
      }
    }
  } catch (error) {
    const errorMessage = (error as Error).message;
    if (!errorMessage.includes('timeout') && !errorMessage.includes('waiting for')) {
      console.log(`Cookie consent handling encountered an issue: ${acceptButtonName} - ${errorMessage}`);
    }
  }
}

export const getS2SToken = async (): Promise<void> => {
  process.env.S2S_URL = s2STokenApiData.s2sUrl;
  process.env.SERVICE_AUTH_TOKEN = await new ServiceAuthUtils().retrieveToken({ microservice: s2STokenApiData.microservice });
}

export const getAccessToken = async (): Promise<void> => {
  process.env.IDAM_WEB_URL = accessTokenApiData.idamUrl;
  process.env.IDAM_TESTING_SUPPORT_URL = accessTokenApiData.idamTestingSupportUrl;
  process.env.BEARER_TOKEN = await new IdamUtils().generateIdamToken({
    username: user.claimantSolicitor.email,
    password: user.claimantSolicitor.password,
    grantType: 'password',
    clientId: 'pcs-api',
    clientSecret: process.env.PCS_API_IDAM_SECRET as string,
    scope: 'profile openid roles'
  });
};

export default globalSetupConfig;
