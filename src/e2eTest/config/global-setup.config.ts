import { chromium } from '@playwright/test';
import { IdamUtils, IdamPage, SessionUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';
import { user } from '@data/user-data';
import { handlePostLoginCookieBanner } from '@utils/cookie.utils';
import {LONG_TIMEOUT, SHORT_TIMEOUT, getMasterStorageStatePath} from '../playwright.config';
import * as path from 'path';
import * as fs from 'fs';

// Session configuration
const SESSION_COOKIE_NAME = 'Idam.Session';


async function globalSetupConfig(): Promise<void> {
  const baseURL = process.env.MANAGE_CASE_BASE_URL;
  if (!baseURL) {
    throw new Error('MANAGE_CASE_BASE_URL environment variable is required');
  }

  // Use master storage state path - this will be copied to worker-specific files
  const storageStatePath = getMasterStorageStatePath();

  // Ensure session directory exists
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

    // Check if valid session exists, skip login if found
    if (fs.existsSync(storageStatePath) && SessionUtils.isSessionValid(storageStatePath, SESSION_COOKIE_NAME)) {
      console.log('✓ Using existing valid session');
      await browser.close();
      return;
    }

    console.log('Performing login and setting up session...');

    await page.goto(baseURL, { waitUntil: 'domcontentloaded', timeout: LONG_TIMEOUT });

    const idamPage = new IdamPage(page);
    await idamPage.login({
      username: userEmail,
      password: userPassword,
      sessionFile: storageStatePath,
    });

    // Wait for successful navigation away from login
    await page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });
    await page.waitForTimeout(SHORT_TIMEOUT); // Wait for Angular components to render

    // Handle post-login cookie banner - non-blocking
    await handlePostLoginCookieBanner(page).catch(() => {
      console.warn('Post-login cookie banner handling failed, continuing with session save...');
    });
    await page.waitForTimeout(SHORT_TIMEOUT);

    // Save storage state to master file
    // Worker-specific files will be created lazily by each worker process
    // using their unique process.pid when they first access ensureWorkerStorageFile()
    await page.context().storageState({ path: storageStatePath });
    console.log('✓ Storage state saved successfully');
    console.log('  Worker-specific files will be created on-demand by each worker process');
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
