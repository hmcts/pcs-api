import { IdamUtils, ServiceAuthUtils } from '@hmcts/playwright-common';
import { chromium } from '@playwright/test';
import { accessTokenApiData, s2STokenApiData } from '@data/api-data';
import { getClaimantSolicitorForSession } from '@data/user-data';
import * as path from 'path';
import * as fs from 'fs';
import { LONG_TIMEOUT } from '../playwright.config';
import { dismissCookieBanner } from '@config/cookie-banner';
import {
  provisionDynamicSolicitorForAlias,
  RuntimeUserAlias,
} from '@utils/test-setup';

const STORAGE_STATE_PATH = path.join(__dirname, '../.auth/storage-state.json');

function ensureIdamEnv(): void {
  process.env.IDAM_WEB_URL = process.env.IDAM_WEB_URL || accessTokenApiData.idamUrl;
  process.env.IDAM_TESTING_SUPPORT_URL =
    process.env.IDAM_TESTING_SUPPORT_URL || accessTokenApiData.idamTestingSupportUrl;
}

async function globalSetupConfig(): Promise<void> {
  ensureIdamEnv();
  await getS2SToken();

  if (process.env.E2E_DYNAMIC_SOLICITOR === 'true') {
    console.log('E2E_DYNAMIC_SOLICITOR: provisioning solicitor user…');
    await provisionDynamicSolicitorForAlias(RuntimeUserAlias.PCS_CLAIMANT_SOLICITOR);
  }

  await getAccessToken();
  await authenticateAndSaveState();
}

async function authenticateAndSaveState(): Promise<string> {
  const baseUrl = process.env.MANAGE_CASE_BASE_URL;

  if (!baseUrl) throw new Error('MANAGE_CASE_BASE_URL is not set');

  const solicitor = getClaimantSolicitorForSession();
  if (!solicitor.email || !solicitor.password) {
    throw new Error('Login failed: missing credentials. Set IDAM_PCS_USER_PASSWORD (and static user or dynamic provisioning).');
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
    await page.locator('#username').fill(solicitor.email);
    await page.locator('#password').fill(solicitor.password);
    await page.locator('#login-submit-btn').click();

    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: LONG_TIMEOUT });

    await page.waitForLoadState('load');

    await dismissCookieBanner(page, 'analytics');

    await page.waitForLoadState('load');

    const cookies = await context.cookies();
    const authCookies = cookies.filter(
      c =>
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
  process.env.S2S_URL = s2STokenApiData.s2sUrl;
  process.env.SERVICE_AUTH_TOKEN = await new ServiceAuthUtils().retrieveToken({
    microservice: s2STokenApiData.microservice,
  });
};

export const getAccessToken = async (): Promise<void> => {
  ensureIdamEnv();
  const tokenUser = getClaimantSolicitorForSession();
  if (!tokenUser.email || !tokenUser.password) {
    throw new Error('getAccessToken: missing claimant solicitor email/password.');
  }
  const idam = new IdamUtils();
  try {
    process.env.BEARER_TOKEN = await idam.generateIdamToken({
      username: tokenUser.email,
      password: tokenUser.password,
      grantType: 'password',
      clientId: 'pcs-api',
      clientSecret: process.env.PCS_API_IDAM_SECRET as string,
      scope: 'profile openid roles',
    });
  } finally {
    await idam.dispose();
  }
};

export default globalSetupConfig;
