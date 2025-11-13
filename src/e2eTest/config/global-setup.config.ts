import { chromium, FullConfig } from '@playwright/test';
import { IdamUtils, IdamPage, SessionUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';
import { user } from '@data/user-data';
import { signInOrCreateAnAccount } from '@data/page-data';
import { CookieUtils } from '@utils/cookie.utils';
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
 * Fallback handler for cookie banners if they appear during global setup
 * Uses the same approach as the working handleCookieConsentAction
 */
async function handleCookieBannerIfPresent(page: any): Promise<void> {
  try {
    // Handle "Accept additional cookies" banner
    const additionalCookiesBanner = page.locator('#cm_cookie_notification');
    const acceptAdditionalBtn = additionalCookiesBanner.getByRole('button', {
      name: signInOrCreateAnAccount.acceptAdditionalCookiesButton,
    });
    const successBanner = page.locator('#accept-all-cookies-success');
    const hideBtn = successBanner.getByRole('button', {
      name: signInOrCreateAnAccount.hideThisCookieMessageButton,
    });

    try {
      await additionalCookiesBanner.waitFor({ state: 'attached', timeout: 5000 });
      await acceptAdditionalBtn.click({ timeout: 5000 });
      await additionalCookiesBanner.waitFor({ state: 'hidden', timeout: 5000 });

      // Handle success banner if it appears
      if (await successBanner.isVisible({ timeout: 5000 }).catch(() => false)) {
        await hideBtn.click({ timeout: 5000 });
        await successBanner.waitFor({ state: 'hidden', timeout: 5000 });
      }
      console.log('Cookie banner: Accepted additional cookies');
    } catch (err) {
      // Banner not present, continue
    }

    // Handle "Accept analytics cookies" banner
    const analyticsBanner = page.locator('xuilib-cookie-banner');
    const acceptAnalyticsBtn = analyticsBanner.getByRole('button', {
      name: signInOrCreateAnAccount.acceptAnalyticsCookiesButton,
    });

    try {
      await analyticsBanner.waitFor({ state: 'attached', timeout: 5000 });
      await acceptAnalyticsBtn.click({ timeout: 5000 });
      await analyticsBanner.waitFor({ state: 'hidden', timeout: 5000 });
      console.log('Cookie banner: Accepted analytics cookies');
    } catch (err) {
      // Banner not present, continue
    }

    await page.waitForLoadState('domcontentloaded', { timeout: 10000 });
  } catch (error) {
    // Silently fail - cookie banner handling is a fallback
    // If cookies were properly injected, banners shouldn't appear
  }
}

async function globalSetupConfig(config: FullConfig): Promise<void> {
  const baseURL = config.projects[0].use?.baseURL || process.env.MANAGE_CASE_BASE_URL || '';
  const storageStatePath = getStorageStatePath();
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    const userEmail = user.claimantSolicitor.email;
    const userPassword = user.claimantSolicitor.password;

    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }

    // Skip login if session is already valid (same logic for CI and local)
    if (SessionUtils.isSessionValid(storageStatePath, SESSION_COOKIE_NAME)) {
      console.log('Valid session found, skipping login...');
      await browser.close();
      return;
    }

    console.log('Performing login and setting up session...');

    // Navigate to login page and perform login
    await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: 30000 });
    
    const idamPage = new IdamPage(page);
    await idamPage.login({
      username: userEmail,
      password: userPassword,
      sessionFile: storageStatePath,
    });

    // Wait for navigation after login
    await page.waitForURL('**/cases', { timeout: 30000 }).catch(async () => {
      await page.waitForFunction(
        () => !window.location.href.includes('/login') && !window.location.href.includes('/sign-in'),
        { timeout: 30000 }
      ).catch(() => null);
    });

    // Wait for page to be ready
    await page.waitForLoadState('domcontentloaded', { timeout: 30000 }).catch(() => {
      return page.waitForLoadState('load', { timeout: 30000 });
    });

    // Handle cookie banners if they appear (before saving session)
    await handleCookieBannerIfPresent(page);

    // Get all cookies from the context after handling banners
    const allCookies = await context.cookies();

    // Add all consent cookies directly to storage state (no UI interaction needed)
    // This prevents cookie banners from appearing in tests
    const cookieUtils = new CookieUtils();
    await cookieUtils.addAllConsentCookies(storageStatePath, baseURL, allCookies);

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
