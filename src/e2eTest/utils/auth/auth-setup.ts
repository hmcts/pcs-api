import { chromium, Browser, BrowserContext, Page } from '@playwright/test';
import { user } from '@data/user-data';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';
import * as path from 'path';
import * as fs from 'fs';

// Path relative to the e2eTest directory
const STORAGE_STATE_PATH = path.join(__dirname, '../../.auth/storage-state.json');

/**
 * Performs login and saves authentication state (cookies, localStorage) to a file
 * This allows all tests to reuse the same authenticated session
 */
export async function authenticateAndSaveState(): Promise<string> {
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
    await page.goto(baseUrl);

    // Handle cookie consent (additional cookies)
    await handleCookieConsent(
      page,
      signInOrCreateAnAccount.acceptAdditionalCookiesButton,
      signInOrCreateAnAccount.hideThisCookieMessageButton
    );

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
    // This might need adjustment based on your actual application behavior
    await page.waitForURL((url) => !url.href.includes('/login') && !url.href.includes('/sign-in'), {
      timeout: 30000,
    }).catch(() => {
      // If URL doesn't change, wait for a specific element that indicates successful login
      // You may need to adjust this selector based on your application
      return page.waitForSelector('body', { timeout: 30000 });
    });

    // Handle analytics cookie consent (if present)
    await handleCookieConsent(
      page,
      signInOrCreateAnAccount.acceptAnalyticsCookiesButton
    );

    // Wait a bit to ensure all cookies/localStorage are set
    await page.waitForTimeout(2000);

    // Save the authentication state
    await context.storageState({ path: STORAGE_STATE_PATH });
    console.log(`✅ Authentication state saved to: ${STORAGE_STATE_PATH}`);

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
 */
async function handleCookieConsent(
  page: Page,
  acceptButtonName: string,
  hideButtonName?: string
): Promise<void> {
  try {
    let consentBanner;
    let acceptBtn;

    if (acceptButtonName === 'Accept additional cookies') {
      consentBanner = page.locator('#cm_cookie_notification');
      acceptBtn = consentBanner.getByRole('button', { name: acceptButtonName });
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

    if (hideButtonName) {
      const successBanner = page.locator('#accept-all-cookies-success');
      const hideBtn = successBanner.getByRole('button', { name: hideButtonName });
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
