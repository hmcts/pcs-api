import { chromium, Browser, BrowserContext, Page } from '@playwright/test';
import { user } from '@data/user-data';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';
import * as path from 'path';
import * as fs from 'fs';
import {home} from "@data/page-data";

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
    // This might need adjustment based on your actual application behavior
    await page.waitForURL((url) => !url.href.includes('/login') && !url.href.includes('/sign-in'), {
      timeout: 30000,
    }).catch(() => {
      // If URL doesn't change, wait for a specific element that indicates successful login
      // You may need to adjust this selector based on your application
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
    // Some cookie consent systems require a navigation to properly save cookies
    await page.goto(baseUrl, { waitUntil: 'domcontentloaded' });
    
    // Wait for page to be ready (use domcontentloaded instead of networkidle for reliability)
    await page.waitForLoadState('domcontentloaded');
    
    // Handle cookie consent again after navigation (in case it appears)
    // This ensures cookies are properly set before saving state
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

    // Wait a bit for the page to fully load
    await page.waitForTimeout(1000);

    // Check if banner is visible with a longer timeout
    const isBannerVisible = await consentBanner.isVisible({ timeout: 5000 }).catch(() => false);

    if (!isBannerVisible) {
      console.log(`ℹ️ Cookie banner not visible: ${acceptButtonName}`);
      return;
    }

    console.log(`🍪 Handling cookie consent: ${acceptButtonName}`);

    // Wait for the accept button to be visible and enabled
    await acceptBtn.waitFor({ state: 'visible', timeout: 15000 });
    await acceptBtn.waitFor({ state: 'attached', timeout: 5000 });
    
    // Scroll into view if needed
    await acceptBtn.scrollIntoViewIfNeeded();
    
    await page.waitForTimeout(500);
    
    // Click the accept button
    await acceptBtn.click({ force: false });
    console.log(`✅ Clicked ${acceptButtonName} button`);

    // Wait for banner to disappear - try multiple strategies
    try {
      await consentBanner.waitFor({ state: 'hidden', timeout: 15000 });
      console.log(`✅ Cookie banner hidden: ${acceptButtonName}`);
    } catch {
      try {
        await consentBanner.waitFor({ state: 'detached', timeout: 15000 });
        console.log(`✅ Cookie banner detached: ${acceptButtonName}`);
      } catch {
        // Check if it's still visible
        const stillVisible = await consentBanner.isVisible().catch(() => false);
        if (stillVisible) {
          console.warn(`⚠️ Cookie banner still visible after clicking: ${acceptButtonName}`);
        }
      }
    }
    
    // Wait longer to ensure cookies are actually set by the browser
    await page.waitForTimeout(2000);

    if (hideButtonName) {
      const successBanner = page.locator('#accept-all-cookies-success');
      const isSuccessBannerVisible = await successBanner.isVisible({ timeout: 5000 }).catch(() => false);

      if (isSuccessBannerVisible) {
        const hideBtn = successBanner.getByRole('button', { name: hideButtonName });
        await hideBtn.waitFor({ state: 'visible', timeout: 10000 });
        await hideBtn.scrollIntoViewIfNeeded();
        await page.waitForTimeout(500);
        await hideBtn.click();
        console.log(`✅ Clicked hide button: ${hideButtonName}`);
        
        await successBanner.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {
          return successBanner.waitFor({ state: 'detached', timeout: 10000 });
        });
      }
    }
  } catch (error) {
    const errorMessage = (error as Error).message;
    console.error(`❌ Cookie consent handling error for ${acceptButtonName}: ${errorMessage}`);
    // Don't throw - we'll try again later
  }
}
