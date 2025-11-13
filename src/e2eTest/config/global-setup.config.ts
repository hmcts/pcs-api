import { chromium, FullConfig, Page } from '@playwright/test';
import { IdamUtils, IdamPage, SessionUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';
import { user } from '@data/user-data';
import * as path from 'path';
import * as fs from 'fs';

// Session configuration
const SESSION_DIR = path.join(process.cwd(), '.auth');
const STORAGE_STATE_FILE = 'storage-state.json';
const SESSION_COOKIE_NAME = process.env.SESSION_COOKIE_NAME || 'Idam.Session';

function getStorageStatePath(): string {
  if (!fs.existsSync(SESSION_DIR)) {
    fs.mkdirSync(SESSION_DIR, { recursive: true });
  }
  return path.join(SESSION_DIR, STORAGE_STATE_FILE);
}

/**
 * Handle pre-login cookie banner (on hmcts-access.service.gov.uk)
 * Banner ID: #cm_cookie_notification
 * Button: "Accept additional cookies" (id: #cookie-accept-submit)
 */
async function handlePreLoginCookieBanner(page: Page): Promise<void> {
  try {
    const cookieBanner = page.locator('#cm_cookie_notification');
    const acceptButton = page.locator('#cookie-accept-submit');

    // Wait for banner to appear (with short timeout as it may not always be present)
    await cookieBanner.waitFor({ state: 'attached', timeout: 5000 }).catch(() => {
      // Banner not present, skip
      return;
    });

    // Check if banner is visible
    if (await cookieBanner.isVisible()) {
      console.log('Pre-login cookie banner detected, accepting cookies...');
      await acceptButton.click({ timeout: 5000 });

      // Wait for banner to be hidden
      await cookieBanner.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {
        // Banner may have disappeared, continue
      });

      // Wait for any success banner and hide it if present
      const successBanner = page.locator('#accept-all-cookies-success');
      const hideButton = successBanner.getByRole('button', { name: 'Hide this cookie message' });

      if (await successBanner.isVisible({ timeout: 2000 }).catch(() => false)) {
        await hideButton.click({ timeout: 2000 }).catch(() => {
          // Hide button may not be present, continue
        });
        await successBanner.waitFor({ state: 'hidden', timeout: 2000 }).catch(() => {
          // Success banner may have disappeared, continue
        });
      }

      console.log('✓ Pre-login cookies accepted');
    }
  } catch (error) {
    // Cookie banner handling is not critical, log and continue
    console.warn('Pre-login cookie banner not found or failed to handle:', (error as Error).message);
  }
}

/**
 * Handle post-login cookie banner (on the service)
 * Banner component: xuilib-cookie-banner
 * Button: "Accept analytics cookies"
 */
async function handlePostLoginCookieBanner(page: Page): Promise<void> {
  try {
    // Wait for page to be fully interactive
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
      // Network idle may not happen, continue
    });

    // Try multiple selectors for the cookie banner (Angular component may render differently)
    const cookieBannerSelectors = [
      'xuilib-cookie-banner',
      'xuilib-cookie-banner .govuk-cookie-banner',
      '[class*="cookie-banner"]',
      'govuk-cookie-banner'
    ];

    let cookieBanner = null;
    let acceptButton = null;

    // Try to find the banner with different selectors
    for (const selector of cookieBannerSelectors) {
      try {
        const banner = page.locator(selector).first();
        await banner.waitFor({ state: 'attached', timeout: 3000 }).catch(() => {});

        if (await banner.isVisible({ timeout: 2000 }).catch(() => false)) {
          cookieBanner = banner;
          // Try to find accept button within the banner
          acceptButton = banner.getByRole('button', { name: /Accept analytics cookies/i });

          // If button not found, try alternative selectors
          if (!(await acceptButton.isVisible({ timeout: 1000 }).catch(() => false))) {
            acceptButton = banner.locator('button:has-text("Accept analytics cookies")');
          }

          if (await acceptButton.isVisible({ timeout: 1000 }).catch(() => false)) {
            console.log(`Post-login cookie banner detected (selector: ${selector}), accepting analytics cookies...`);
            break;
          }
        }
      } catch {
        // Try next selector
      }
    }

    if (cookieBanner && acceptButton && await cookieBanner.isVisible({ timeout: 2000 }).catch(() => false)) {
      await acceptButton.click({ timeout: 5000 });

      // Wait for banner to be hidden or removed
      await cookieBanner.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {
        // Try detached state as well
        return cookieBanner.waitFor({ state: 'detached', timeout: 5000 }).catch(() => {});
      });

      console.log('✓ Post-login analytics cookies accepted');
    } else {
      console.log('Post-login cookie banner not found or already dismissed');
    }
  } catch (error) {
    // Cookie banner handling is not critical, log and continue
    console.warn('Post-login cookie banner not found or failed to handle:', (error as Error).message);
  }
}

async function globalSetupConfig(config: FullConfig): Promise<void> {
  const baseURL = config.projects[0].use?.baseURL || process.env.MANAGE_CASE_BASE_URL || '';
  const storageStatePath = getStorageStatePath();
  const browser = await chromium.launch();
  const page = await browser.newPage();

  try {
    const userEmail = user.claimantSolicitor.email;
    const userPassword = user.claimantSolicitor.password;

    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }

    if (SessionUtils.isSessionValid(storageStatePath, SESSION_COOKIE_NAME)) {
      console.log('Valid session found, skipping login...');
      await browser.close();
      return;
    }

    console.log('Performing login and setting up session...');

    await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: 30000 });

    // Handle pre-login cookie banner (before login)
    await handlePreLoginCookieBanner(page);

    const idamPage = new IdamPage(page);
    await idamPage.login({
      username: userEmail,
      password: userPassword,
      sessionFile: storageStatePath,
    });

    // Wait for successful navigation away from login
    await page.waitForFunction(
      () => !window.location.href.includes('/login') && !window.location.href.includes('/sign-in'),
      { timeout: 30000 }
    );

    // Wait for page to be fully loaded
    await page.waitForLoadState('domcontentloaded', { timeout: 30000 });

    // Wait a bit more for Angular components to render (cookie banner is an Angular component)
    await page.waitForTimeout(2000);

    // Handle post-login cookie banner (after successful login)
    await handlePostLoginCookieBanner(page);

    // Wait a bit more after accepting cookies to ensure they're saved
    await page.waitForTimeout(1000);

    // Save storage state again after navigation and cookie acceptance to ensure all cookies are captured
    // IdamPage.login may save before all cookies are set, and cookie acceptance adds additional cookies
    await page.context().storageState({ path: storageStatePath });

    if (!fs.existsSync(storageStatePath)) {
      throw new Error(`Storage state file was not created at ${storageStatePath}`);
    }

    // Verify storage state contains session cookie
    const storageStateContent = JSON.parse(fs.readFileSync(storageStatePath, 'utf-8'));
    const hasSessionCookie = storageStateContent.cookies?.some(
      (cookie: { name: string }) => cookie.name === SESSION_COOKIE_NAME
    );

    if (!hasSessionCookie) {
      console.error(`ERROR: Storage state created but ${SESSION_COOKIE_NAME} cookie not found!`);
      console.log('Cookies in storage state:', storageStateContent.cookies?.map((c: { name: string }) => c.name) || 'none');
      console.log('Storage state path:', storageStatePath);
      throw new Error(`Session cookie ${SESSION_COOKIE_NAME} not found in storage state. Login may have failed.`);
    } else {
      console.log(`✓ Storage state created with ${SESSION_COOKIE_NAME} cookie`);
      console.log(`✓ Total cookies saved: ${storageStateContent.cookies?.length || 0}`);
      console.log(`✓ Storage state file: ${storageStatePath}`);
    }

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
