import { IdamUtils, ServiceAuthUtils } from '@hmcts/playwright-common';
import { chromium, Page } from '@playwright/test';
import { accessTokenApiData, s2STokenApiData } from '@data/api-data';
import { user } from '@data/user-data';
import * as path from 'path';
import * as fs from 'fs';

const STORAGE_STATE_PATH = path.join(__dirname, '../.auth/storage-state.json');

async function globalSetupConfig(): Promise<void> {
  await getAccessToken();
  await getS2SToken();
  await authenticateAndSaveState();
}

async function authenticateAndSaveState(): Promise<string> {
  const baseUrl = process.env.MANAGE_CASE_BASE_URL;
  const userPassword = user.claimantSolicitor.password || process.env.IDAM_PCS_USER_PASSWORD;

  if (!baseUrl) throw new Error('MANAGE_CASE_BASE_URL is not set');
  if (!user.claimantSolicitor.email || !userPassword) {
    throw new Error('Login failed: missing credentials. Set IDAM_PCS_USER_PASSWORD.');
  }

  const authDir = path.dirname(STORAGE_STATE_PATH);
  fs.mkdirSync(authDir, { recursive: true });

  const browser = await chromium.launch({ headless: !!process.env.CI });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();

  try {
    await page.goto(baseUrl, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    await dismissCookieBanner(page, 'additional');
    await dismissCookieBanner(page, 'analytics');

    await page.waitForSelector('#username', { timeout: 30000 });
    await page.locator('#username').fill(user.claimantSolicitor.email);
    await page.locator('#password').fill(userPassword);
    await page.locator('#login-submit-btn').click();

    await page.waitForURL((url) => !url.href.includes('/login') && !url.href.includes('/sign-in'), { timeout: 30000 });

    await dismissCookieBanner(page, 'additional');
    await dismissCookieBanner(page, 'analytics');

    await page.waitForTimeout(1500);
    await context.storageState({ path: STORAGE_STATE_PATH });

    return STORAGE_STATE_PATH;
  } catch (error) {
    console.error('❌ Authentication setup failed:', error);
    await page.screenshot({ path: path.join(authDir, 'auth-failure.png'), fullPage: true });
    throw error;
  } finally {
    await browser.close();
  }
}

/**
 * Dismiss cookie banner. Before login: #cookie-accept-submit (hmcts-access).
 * After login: button "Accept analytics cookies" in .govuk-cookie-banner__message.
 */
async function dismissCookieBanner(page: Page, type: 'additional' | 'analytics'): Promise<void> {
  try {
    const btn = type === 'additional'
      ? page.locator('#cookie-accept-submit')
      : page.locator('.govuk-cookie-banner__message').getByRole('button', { name: /Accept analytics cookies/i });
    if (await btn.isVisible().catch(() => false)) {
      await btn.click();
    }
  } catch {
    // Ignore – banner may not be present
  }
}

export const getS2SToken = async (): Promise<void> => {
  process.env.S2S_URL = s2STokenApiData.s2sUrl;
  process.env.SERVICE_AUTH_TOKEN = await new ServiceAuthUtils().retrieveToken({ microservice: s2STokenApiData.microservice });
};

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
