import {IdamUtils, ServiceAuthUtils} from '@hmcts/playwright-common';
import type {BrowserContext} from '@playwright/test';
import {chromium, expect} from '@playwright/test';
import {user} from '@data/user-data';
import * as path from 'path';
import * as fs from 'fs';
import {LONG_TIMEOUT} from "../playwright.config";
import { dismissCookieBanner } from '@config/cookie-banner';

const STORAGE_STATE_PATH = path.join(__dirname, '../.auth/storage-state.json');

/** Matches Jenkins nightly `E2E_TARGET_ENV` and full slug URL templates. */
const NIGHTLY_ENV_SLUGS = new Set(['aat', 'demo', 'perftest', 'ithc']);

function applyPlaywrightServiceUrls(): void {
  const e = (process.env.ENVIRONMENT || '').toLowerCase();

  if (NIGHTLY_ENV_SLUGS.has(e)) {
    process.env.MANAGE_CASE_BASE_URL ||= `https://manage-case.${e}.platform.hmcts.net`;
    process.env.DATA_STORE_URL_BASE ||= `http://ccd-data-store-api-${e}.service.core-compute-${e}.internal`;
    process.env.IDAM_WEB_URL ||= `https://idam-api.${e}.platform.hmcts.net`;
    process.env.IDAM_TESTING_SUPPORT_URL ||= `https://idam-testing-support-api.${e}.platform.hmcts.net`;
    process.env.S2S_URL ||= `http://rpe-service-auth-provider-${e}.service.core-compute-${e}.internal/testing-support/lease`;
    process.env.CASE_API_URL ||= `http://pcs-api-${e}.service.core-compute-${e}.internal`;
    process.env.DM_STORE ||= `http://dm-store-${e}.service.core-compute-${e}.internal`;
  } else {
    // preview, empty ENVIRONMENT, etc.: AAT IdAM/S2S (same as Jenkinsfile_CNP defaults). MANAGE_CASE / data-store from Jenkins or exports.
    process.env.IDAM_WEB_URL ||= 'https://idam-api.aat.platform.hmcts.net';
    process.env.IDAM_TESTING_SUPPORT_URL ||= 'https://idam-testing-support-api.aat.platform.hmcts.net';
    process.env.S2S_URL ||= 'http://rpe-service-auth-provider-aat.service.core-compute-aat.internal/testing-support/lease';
    process.env.DM_STORE ||= `http://dm-store-aat.service.core-compute-aat.internal`
  }
}

async function globalSetupConfig(): Promise<void> {
  applyPlaywrightServiceUrls();
  await getAccessToken();
  await getS2SToken();
  await authenticateAndSaveState();
}

async function authenticateAndSaveState(): Promise<string> {
  const baseUrl = process.env.MANAGE_CASE_BASE_URL;

  if (!baseUrl) {
    throw new Error(
      'MANAGE_CASE_BASE_URL is not set (export it, or set ENVIRONMENT to aat|demo|perftest|ithc for default manage-case URL).'
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

    await page.waitForSelector('#email', { timeout: LONG_TIMEOUT });
    await page.locator('#email').fill(user.claimantSolicitor.email);
    await page.getByRole('button', { name: 'Continue' }).click(); 
    const pwdHeader = page.getByLabel('Enter your password', { exact: true });
    await expect(pwdHeader).toBeVisible({ timeout: LONG_TIMEOUT });
    await page.locator('#password').fill(user.claimantSolicitor.password);
    await page.getByRole('button', { name: 'Continue' }).click(); 

    await page.waitForURL((url) => !url.href.includes('/enter-password'), { timeout: LONG_TIMEOUT });

    await page.waitForLoadState('load');

    await dismissCookieBanner(page, 'analytics');

    await page.waitForLoadState('load');

    await presetCookieBannerForAllUsers(context, baseUrl);

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

/**
 * Pre-accepts XUI's cookie banner for every test user, so no test ever has to wait for it.
 *
 * XUI keys the banner off `hmcts-exui-cookies-${userId}-mc-accepted`
 * (rpx-xui-webapp app.component.ts:185, where userId is `userInfo.id ?? userInfo.uid`). Because
 * the name embeds the user id, the storage state saved above only suppresses the banner for the
 * one user this setup logs in as — every other user still meets it, which is why
 * dismissCookieBanner is called in most specs and why one run logged 8 presence-check misses.
 *
 * Resolving the ids is cheap: the suite already queries IDAM testing-support by email.
 *
 * Deliberately non-fatal. If an id cannot be resolved the banner simply still appears for that
 * user and dismissCookieBanner handles it as before — this is an optimisation, and it must not
 * be able to fail the whole run. Each resolution is logged so a silent no-op is visible rather
 * than looking like success.
 */
async function presetCookieBannerForAllUsers(context: BrowserContext, baseUrl: string): Promise<void> {
  const emails = [...new Set(Object.values(user)
    .map(u => (u as { email?: string })?.email)
    .filter((e): e is string => typeof e === 'string' && e.length > 0))];

  const domain = new URL(baseUrl).hostname;
  const expires = Math.floor(Date.now() / 1000) + 60 * 60 * 24 * 365;
  const resolved: string[] = [];
  const unresolved: string[] = [];

  for (const email of emails) {
    try {
      const id = await resolveIdamUserId(email);
      if (!id) {
        unresolved.push(email);
        continue;
      }
      await context.addCookies([{
        name: `hmcts-exui-cookies-${id}-mc-accepted`,
        value: 'true',
        domain,
        path: '/',
        expires,
      }]);
      resolved.push(email);
    } catch (err) {
      unresolved.push(`${email} (${err instanceof Error ? err.message : err})`);
    }
  }

  console.log(`[cookie-preset] pre-accepted the XUI banner for ${resolved.length}/${emails.length} users on ${domain}`);
  if (unresolved.length) {
    console.warn(`[cookie-preset] could not resolve: ${unresolved.join(', ')} — those users will still see the banner`);
  }

  await presetIdamCookiePolicy(context);
}

/**
 * Pre-accepts the IDAM login page's cookie banner (`#accept-additional-cookies`).
 *
 * Name and host established empirically in #2642 rather than from source, because the app that
 * serves this banner is not cloned in this workspace — the only reference to the button id
 * anywhere here is our own test helper. The dump showed:
 *
 *   cookies_policy@idam-web-public.aat.platform.hmcts.net
 *
 * Note the host is `idam-web-public`, which is *not* derivable from `IDAM_WEB_URL`
 * (`idam-api...`) — a different subdomain — so it is derived from the environment slug instead.
 *
 * The value is the GOV.UK cookie-policy shape. If it turns out the banner keys off specific
 * fields rather than mere presence, the fallback is unchanged behaviour: the banner still renders
 * and dismissCookieBanner clicks it, just after a shortened probe.
 */
async function presetIdamCookiePolicy(context: BrowserContext): Promise<void> {
  const env = (process.env.ENVIRONMENT || '').toLowerCase();
  const slug = NIGHTLY_ENV_SLUGS.has(env) ? env : 'aat';
  const domain = `idam-web-public.${slug}.platform.hmcts.net`;

  try {
    await context.addCookies([{
      name: 'cookies_policy',
      value: JSON.stringify({ essential: true, analytics: false, apm: false }),
      domain,
      path: '/',
      expires: Math.floor(Date.now() / 1000) + 60 * 60 * 24 * 365,
    }]);
    console.log(`[cookie-preset] pre-accepted the IDAM banner on ${domain}`);
  } catch (err) {
    console.warn(`[cookie-preset] could not pre-set cookies_policy on ${domain}: ${err instanceof Error ? err.message : err}`);
  }
}

/**
 * Returns the IDAM user id for an email, or null.
 *
 * Handles both response shapes because the endpoint's contract is not obvious from the existing
 * callers: fetchCurrentUserAPI reads `.data.displayName` as if it were an object, but an
 * `?email=` query can equally return an array. Accepting both means a shape change surfaces as
 * an unresolved user in the log rather than a silently wrong cookie name.
 */
async function resolveIdamUserId(email: string): Promise<string | null> {
  const base = process.env.IDAM_TESTING_SUPPORT_URL;
  if (!base || !process.env.BEARER_TOKEN) {
    return null;
  }
  const response = await fetch(`${base}/test/idam/users?email=${encodeURIComponent(email)}`, {
    headers: { Authorization: `Bearer ${process.env.BEARER_TOKEN}`, Accept: 'application/json' },
  });
  if (!response.ok) {
    return null;
  }
  const body = await response.json() as unknown;
  const record = Array.isArray(body) ? body[0] : body;
  const id = (record as { id?: string; uid?: string } | undefined);
  return id?.id ?? id?.uid ?? null;
}

export const getS2SToken = async (): Promise<void> => {
  if (!process.env.S2S_URL) {
    throw new Error('S2S_URL is not set (set ENVIRONMENT to aat|demo|perftest|ithc, or preview/empty for AAT default, or export S2S_URL)');
  }
  process.env.SERVICE_AUTH_TOKEN = await new ServiceAuthUtils().retrieveToken({ microservice: 'pcs_api' });
};

export const getAccessToken = async (): Promise<void> => {
  if (!process.env.IDAM_WEB_URL || !process.env.IDAM_TESTING_SUPPORT_URL) {
    throw new Error(
      'IDAM_WEB_URL and IDAM_TESTING_SUPPORT_URL are not set (set ENVIRONMENT to aat|demo|perftest|ithc, preview defaults AAT, or export both URLs)'
    );
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
