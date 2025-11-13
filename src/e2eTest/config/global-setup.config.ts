import { chromium, FullConfig } from '@playwright/test';
import { IdamUtils, IdamPage, SessionUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';
import { user } from '@data/user-data';
import { handlePreLoginCookieBanner, handlePostLoginCookieBanner } from '@utils/cookie.utils';
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
  const page = await browser.newPage();

  try {
    const userEmail = user.claimantSolicitor.email;
    const userPassword = user.claimantSolicitor.password;

    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }

    // Check if valid session exists, skip login if found
    if (fs.existsSync(storageStatePath) && SessionUtils.isSessionValid(storageStatePath, SESSION_COOKIE_NAME)) {
      const storageStateContent = JSON.parse(fs.readFileSync(storageStatePath, 'utf-8'));
      const hasSessionCookie = storageStateContent.cookies?.some(
        (cookie: { name: string }) => cookie.name === SESSION_COOKIE_NAME
      );
      if (hasSessionCookie) {
        console.log(`✓ Using existing session with ${storageStateContent.cookies?.length || 0} cookies`);
        await browser.close();
        return;
      }
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

    // Wait longer after accepting cookies to ensure they're fully saved by the browser
    // Cookie consent may trigger additional network requests to save preferences
    await page.waitForTimeout(3000);
    
    // Wait for any pending network requests related to cookie consent
    await page.waitForLoadState('networkidle', { timeout: 5000 }).catch(() => {
      // Network idle may not happen, continue
    });

    // Save storage state with atomic write (write to temp file, then rename)
    const tempStorageStatePath = storageStatePath + '.tmp';
    await page.context().storageState({ path: tempStorageStatePath });
    fs.renameSync(tempStorageStatePath, storageStatePath);
    await new Promise(resolve => setTimeout(resolve, 100)); // Ensure file system flush

    // Verify session cookie was saved
    const storageStateContent = JSON.parse(fs.readFileSync(storageStatePath, 'utf-8'));
    const allCookies = storageStateContent.cookies || [];
    const hasSessionCookie = allCookies.some(
      (cookie: { name: string }) => cookie.name === SESSION_COOKIE_NAME
    );

    if (!hasSessionCookie) {
      throw new Error(`Session cookie ${SESSION_COOKIE_NAME} not found in storage state. Login may have failed.`);
    }

    console.log(`✓ Storage state created with ${SESSION_COOKIE_NAME} cookie (${allCookies.length} total cookies)`);
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
