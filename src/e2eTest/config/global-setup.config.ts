import { chromium } from '@playwright/test';
import { IdamUtils, IdamPage, SessionUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';
import { user } from '@data/user-data';
import { handlePostLoginCookieBanner } from '@utils/cookie.utils';
import {LONG_TIMEOUT, SHORT_TIMEOUT, getStorageStatePath, SESSION_COOKIE_NAME} from '../playwright.config';
import * as path from 'path';
import * as fs from 'fs';


async function globalSetupConfig(): Promise<void> {
  const baseURL = process.env.MANAGE_CASE_BASE_URL;
  if (!baseURL) {
    throw new Error('MANAGE_CASE_BASE_URL environment variable is required');
  }

  const storageStatePath = getStorageStatePath();
  const sessionDir = path.dirname(storageStatePath);
  fs.mkdirSync(sessionDir, { recursive: true });
  const browser = await chromium.launch({ channel: 'chrome', headless: !!process.env.CI });
  const page = await browser.newPage();

  try {
    const userEmail = user.claimantSolicitor.email;
    const userPassword = user.claimantSolicitor.password;

    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }

    const sessionExists = fs.existsSync(storageStatePath);
    const sessionValid = sessionExists && SessionUtils.isSessionValid(storageStatePath, SESSION_COOKIE_NAME);

    if (sessionValid) {
      await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });
      const isLogin = await page.locator('#username').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);
      if (!isLogin) {
        await browser.close();
        return;
      }
    }

    await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });

    const idamPage = new IdamPage(page);
    await idamPage.login({
      username: userEmail,
      password: userPassword,
      sessionFile: storageStatePath,
    });

    // Wait for navigation to complete after login (critical for CI)
    await page.waitForLoadState('networkidle', { timeout: LONG_TIMEOUT }).catch(() => {
      // Fallback to domcontentloaded if networkidle times out
      return page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });
    });
    
    // Verify login was successful - ensure we're not still on login page
    await page.waitForTimeout(1000); // Wait for any redirects
    const currentUrl = page.url().toLowerCase();
    const isStillOnLoginPage = currentUrl.includes('/login') || 
                               currentUrl.includes('/idam') || 
                               currentUrl.includes('/auth') || 
                               currentUrl.includes('/sign-in') ||
                               await page.locator('#username').isVisible({ timeout: SHORT_TIMEOUT }).catch(() => false);
    
    if (isStillOnLoginPage) {
      throw new Error('Login failed: Still on login page after authentication attempt');
    }
    
    await handlePostLoginCookieBanner(page).catch(() => {});
    
    // Save storage state
    await page.context().storageState({ path: storageStatePath });
    
    // Verify storage state file was created (critical for CI)
    if (!fs.existsSync(storageStatePath)) {
      throw new Error(`Storage state file was not created at ${storageStatePath}`);
    }
    
    // Verify session cookie exists in storage state (critical for CI)
    const storageStateContent = JSON.parse(fs.readFileSync(storageStatePath, 'utf-8'));
    const hasSessionCookie = storageStateContent.cookies?.some(
      (cookie: any) => cookie.name === SESSION_COOKIE_NAME && cookie.value
    );
    
    if (!hasSessionCookie) {
      throw new Error(`Session cookie ${SESSION_COOKIE_NAME} not found in storage state`);
    }
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
