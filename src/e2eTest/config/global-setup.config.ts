import { chromium, FullConfig } from '@playwright/test';
import { IdamUtils, IdamPage, SessionUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';
import { user } from '@data/user-data';
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

    // Add all consent cookies directly to storage state (no UI interaction needed)
    // This prevents cookie banners from appearing in tests
    const cookieUtils = new CookieUtils();
    await cookieUtils.addAllConsentCookies(storageStatePath, baseURL);

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
