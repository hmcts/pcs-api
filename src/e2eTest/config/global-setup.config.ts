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

    // Check if storage state file exists and validate session
    if (fs.existsSync(storageStatePath)) {
      try {
        if (SessionUtils.isSessionValid(storageStatePath, SESSION_COOKIE_NAME)) {
          console.log('Valid session found, skipping login...');
          // Verify the storage state file is readable and contains cookies
          const storageStateContent = JSON.parse(fs.readFileSync(storageStatePath, 'utf-8'));
          const hasSessionCookie = storageStateContent.cookies?.some(
            (cookie: { name: string }) => cookie.name === SESSION_COOKIE_NAME
          );
          if (hasSessionCookie) {
            console.log(`✓ Using existing session with ${storageStateContent.cookies?.length || 0} cookies`);
            await browser.close();
            return;
          } else {
            console.log('Storage state exists but session cookie missing, will re-login...');
          }
        } else {
          console.log('Session expired or invalid, will re-login...');
        }
      } catch (error) {
        console.warn('Error validating session, will re-login:', (error as Error).message);
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

    // Get all cookies from the page context before saving
    const cookiesBeforeSave = await page.context().cookies();
    console.log(`Cookies in context before saving: ${cookiesBeforeSave.length}`);
    console.log(`Cookie names: ${cookiesBeforeSave.map(c => c.name).join(', ')}`);

    // Save storage state with atomic write to prevent workers from reading partial file
    // Write to temp file first, then rename (atomic operation on most filesystems)
    const tempStorageStatePath = storageStatePath + '.tmp';
    await page.context().storageState({ path: tempStorageStatePath });
    
    // Verify temp file was created
    if (!fs.existsSync(tempStorageStatePath)) {
      throw new Error(`Temporary storage state file was not created at ${tempStorageStatePath}`);
    }
    
    // Atomic move: rename temp file to final file (prevents workers from reading partial file)
    fs.renameSync(tempStorageStatePath, storageStatePath);
    
    // Small delay to ensure file system has flushed the write
    await new Promise(resolve => setTimeout(resolve, 100));

    if (!fs.existsSync(storageStatePath)) {
      throw new Error(`Storage state file was not created at ${storageStatePath}`);
    }

    // Verify storage state contains session cookie and log all cookies
    const storageStateContent = JSON.parse(fs.readFileSync(storageStatePath, 'utf-8'));
    const allCookies = storageStateContent.cookies || [];
    const hasSessionCookie = allCookies.some(
      (cookie: { name: string }) => cookie.name === SESSION_COOKIE_NAME
    );

    // Check for cookie consent cookies (based on actual cookie names from the application)
    const consentCookieNames = [
      'cookies_preferences_set',  // Actual cookie name from IDAM
      'cookies_policy',           // Actual cookie name from IDAM
      'hmcts-exui-cookies',       // Actual cookie name from XUI (partial match)
      'seen_cookie_message',      // Cookie banner seen flag
      'cookie-preferences',       // Alternative format
      'cookies-preferences',      // Alternative format
      'cookie_policy',            // Alternative format
      'cookiePolicy',             // Alternative format
      'analytics',                // Analytics cookies
      'cookieConsent'             // Alternative format
    ];
    const hasConsentCookies = allCookies.some(
      (cookie: { name: string }) => consentCookieNames.some(name => 
        cookie.name.toLowerCase().includes(name.toLowerCase()) || 
        cookie.name.toLowerCase().startsWith(name.toLowerCase())
      )
    );

    if (!hasSessionCookie) {
      console.error(`ERROR: Storage state created but ${SESSION_COOKIE_NAME} cookie not found!`);
      console.log('Cookies in storage state:', allCookies.map((c: { name: string }) => c.name) || 'none');
      console.log('Storage state path:', storageStatePath);
      throw new Error(`Session cookie ${SESSION_COOKIE_NAME} not found in storage state. Login may have failed.`);
    } else {
      console.log(`✓ Storage state created with ${SESSION_COOKIE_NAME} cookie`);
      console.log(`✓ Total cookies saved: ${allCookies.length}`);
      
      if (hasConsentCookies) {
        const consentCookies = allCookies.filter((c: { name: string }) => 
          consentCookieNames.some(name => 
            c.name.toLowerCase().includes(name.toLowerCase()) || 
            c.name.toLowerCase().startsWith(name.toLowerCase())
          )
        );
        console.log(`✓ Cookie consent cookies found: ${consentCookies.map((c: { name: string }) => c.name).join(', ')}`);
      } else {
        console.warn(`⚠ Warning: Cookie consent cookies not found in storage state. Cookie banner may appear in tests.`);
        console.log('All cookies:', allCookies.map((c: { name: string; domain?: string; path?: string }) => 
          `${c.name} (domain: ${c.domain || 'N/A'}, path: ${c.path || 'N/A'})`
        ).join(', '));
      }
      
      console.log(`✓ Storage state file: ${storageStatePath}`);
    }

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
