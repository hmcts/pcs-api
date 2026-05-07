import {IdamUtils, ServiceAuthUtils} from '@hmcts/playwright-common';
import {chromium} from '@playwright/test';
import {user} from '@data/user-data';
import * as path from 'path';
import * as fs from 'fs';
import {LONG_TIMEOUT} from "../playwright.config";
import { dismissCookieBanner } from '@config/cookie-banner';

const STORAGE_STATE_PATH = path.join(__dirname, '../.auth/storage-state.json');

const STANDARD_CNP_ENVS = new Set(['aat', 'demo', 'perftest', 'ithc']);

/**
 * For aat | demo | perftest | ithc, fills standard HMCTS URLs from ENVIRONMENT so local/CI only need ENVIRONMENT + secrets.
 * Does not overwrite vars already set (e.g. preview or manual overrides).
 */
function applyStandardHmctsUrlsFromEnvironment(): void {
  const e = (process.env.ENVIRONMENT || '').toLowerCase();
  if (!STANDARD_CNP_ENVS.has(e)) {
    return;
  }
  process.env.MANAGE_CASE_BASE_URL ||= `https://manage-case.${e}.platform.hmcts.net`;
  process.env.DATA_STORE_URL_BASE ||= `http://ccd-data-store-api-${e}.service.core-compute-${e}.internal`;
  process.env.IDAM_WEB_URL ||= `https://idam-api.${e}.platform.hmcts.net`;
  process.env.IDAM_TESTING_SUPPORT_URL ||= `https://idam-testing-support-api.${e}.platform.hmcts.net`;
  process.env.S2S_URL ||= `http://rpe-service-auth-provider-${e}.service.core-compute-${e}.internal/testing-support/lease`;
}

async function globalSetupConfig(): Promise<void> {
  applyStandardHmctsUrlsFromEnvironment();
  await getAccessToken();
  await getS2SToken();
  await authenticateAndSaveState();
}

async function authenticateAndSaveState(): Promise<string> {
  const baseUrl = process.env.MANAGE_CASE_BASE_URL;

  if (!baseUrl) {
    throw new Error(
      'MANAGE_CASE_BASE_URL is not set. For aat/demo/perftest/ithc set ENVIRONMENT (e.g. ENVIRONMENT=aat); for preview set MANAGE_CASE_BASE_URL explicitly.'
    );
  }
  if (!user.claimantSolicitor.email || !user.claimantSolicitor.password) {
    throw new Error('Login failed: missing credentials. Set IDAM_PCS_USER_PASSWORD.');
  }

  const authDir = path.dirname(STORAGE_STATE_PATH);
  fs.mkdirSync(authDir, { recursive: true });

  const browser = await chromium.launch({ headless: !!process.env.CI });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();

  try {
    await page.goto(baseUrl, { waitUntil: 'domcontentloaded' });

    await dismissCookieBanner(page, 'additional');

    await page.waitForSelector('#username', { timeout: LONG_TIMEOUT });
    await page.locator('#username').fill(user.claimantSolicitor.email);
    await page.locator('#password').fill(user.claimantSolicitor.password);
    await page.locator('#login-submit-btn').click();

    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: LONG_TIMEOUT });

    await page.waitForLoadState('load');

    await dismissCookieBanner(page, 'analytics');

    await page.waitForLoadState('load');

    const cookies = await context.cookies();
    const authCookies = cookies.filter(c =>
      c.name.includes('auth') ||
      c.name.includes('session') ||
      c.name.includes('token') ||
      c.name === 'Idam.Session' ||
      c.name === '__auth__'
    );

    if (authCookies.length === 0) {
      throw new Error('No authentication cookies found after login. Login may have failed.');
    }

    await context.storageState({ path: STORAGE_STATE_PATH });

    if (!fs.existsSync(STORAGE_STATE_PATH)) {
      throw new Error(`Storage state file was not created at ${STORAGE_STATE_PATH}`);
    }

    const savedState = JSON.parse(fs.readFileSync(STORAGE_STATE_PATH, 'utf-8'));
    console.log(`Authentication state saved: ${savedState.cookies?.length || 0} cookies`);

    return STORAGE_STATE_PATH;
  } catch (error) {
    console.error('Authentication setup failed:', error);
    await page.screenshot({ path: path.join(authDir, 'auth-failure.png'), fullPage: true });
    throw error;
  } finally {
    await browser.close();
  }
}

export const getS2SToken = async (): Promise<void> => {
  if (!process.env.S2S_URL) {
    throw new Error('S2S_URL is not set (use ENVIRONMENT=aat|demo|perftest|ithc or set S2S_URL)');
  }
  process.env.SERVICE_AUTH_TOKEN = await new ServiceAuthUtils().retrieveToken({ microservice: 'pcs_api' });
};

export const getAccessToken = async (): Promise<void> => {
  if (!process.env.IDAM_WEB_URL || !process.env.IDAM_TESTING_SUPPORT_URL) {
    throw new Error('IDAM_WEB_URL and IDAM_TESTING_SUPPORT_URL are not set (use ENVIRONMENT or set both explicitly)');
  }
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
