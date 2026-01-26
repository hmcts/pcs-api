import {IdamUtils, ServiceAuthUtils} from '@hmcts/playwright-common';
import {chromium, Page} from '@playwright/test';
import {accessTokenApiData, s2STokenApiData} from '@data/api-data';
import {user} from '@data/user-data';
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
  const userPassword = user.claimantSolicitor.password;

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
    await dismissCookieBanner(page, 'hide-success');

    await page.waitForSelector('#username', { timeout: 30000 });
    await page.locator('#username').fill(user.claimantSolicitor.email);
    await page.locator('#password').fill(userPassword);
    await page.locator('#login-submit-btn').click();

    await page.waitForURL((url) => !url.href.includes('/login') && !url.href.includes('/sign-in'), { timeout: 30000 });

    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    await dismissCookieBanner(page, 'analytics');
    await dismissCookieBanner(page, 'hide-success');

    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

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

async function dismissCookieBanner(page: Page, type: 'additional' | 'analytics' | 'hide-success'): Promise<void> {
  try {
    let btn;
    if (type === 'additional') {
      btn = page.locator('#cookie-accept-submit');
    } else if (type === 'analytics') {
      btn = page.locator('.govuk-button-group').getByRole('button', { name: 'Accept analytics cookies' });
    } else if (type === 'hide-success') {
      btn = page.locator('#cookie-accept-all-success-banner-hide');
    }

    if (btn) {
      const isVisible = await btn.isVisible({ timeout: 5000 }).catch(() => false);
      if (isVisible) {
        await btn.scrollIntoViewIfNeeded();
        await btn.click();
        await page.waitForTimeout(500);
      }
    }
  } catch {
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
