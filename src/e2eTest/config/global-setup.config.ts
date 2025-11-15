import { chromium } from '@playwright/test';
import { IdamUtils, IdamPage, SessionUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';
import { user } from '@data/user-data';
import { handlePostLoginCookieBanner } from '@utils/cookie.utils';
import {LONG_TIMEOUT, SHORT_TIMEOUT, getMasterStorageStatePath, SESSION_COOKIE_NAME} from '../playwright.config';
import * as path from 'path';
import * as fs from 'fs';


async function globalSetupConfig(): Promise<void> {
  const baseURL = process.env.MANAGE_CASE_BASE_URL;
  if (!baseURL) {
    throw new Error('MANAGE_CASE_BASE_URL environment variable is required');
  }

  const storageStatePath = getMasterStorageStatePath();
  const sessionDir = path.dirname(storageStatePath);
  if (!fs.existsSync(sessionDir)) {
    fs.mkdirSync(sessionDir, { recursive: true });
  }
  const browser = await chromium.launch({headless: !!process.env.CI});
  const page = await browser.newPage();

  try {
    const userEmail = user.claimantSolicitor.email;
    const userPassword = user.claimantSolicitor.password;

    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }

    if (fs.existsSync(storageStatePath) && SessionUtils.isSessionValid(storageStatePath, SESSION_COOKIE_NAME)) {
      await browser.close();
      return;
    }

    await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });

    const idamPage = new IdamPage(page);
    await idamPage.login({
      username: userEmail,
      password: userPassword,
      sessionFile: storageStatePath,
    });

    await page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });
    await page.waitForTimeout(SHORT_TIMEOUT);
    await handlePostLoginCookieBanner(page).catch(() => {});
    await page.waitForTimeout(SHORT_TIMEOUT);
    await page.context().storageState({ path: storageStatePath });
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
