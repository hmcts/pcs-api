import * as fs from 'fs';
import * as path from 'path';
import { expect, test } from '@utils/test-fixtures';
import { initializeExecutor, performAction } from '@utils/controller';
import { dismissCookieBanner } from '@config/cookie-banner';
import { home } from '@data/page-data';

/**
 * Perf User Refresh — one Playwright test per username from:
 *   src/e2eTest/data/inputFiles/Perf_UserRefresh_Input.csv
 *
 * Journey: Manage Case URL → login → verify successful login.
 * Password: existing framework env IDAM_PCS_USER_PASSWORD (not from CSV).
 * Results: standard Playwright / Jenkins E2E reporting only.
 */

const PERF_USER_REFRESH_CSV = path.resolve(
  __dirname,
  '../data/inputFiles/Perf_UserRefresh_Input.csv'
);

function loadPerfUserRefreshUsernames(csvPath: string): string[] {
  if (!fs.existsSync(csvPath)) {
    throw new Error(
      `Perf user refresh input file not found: ${csvPath}. ` +
        `Expected src/e2eTest/data/inputFiles/Perf_UserRefresh_Input.csv`
    );
  }

  const raw = fs.readFileSync(csvPath, 'utf-8').replace(/^\uFEFF/, '');
  const lines = raw
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  if (lines.length < 2) {
    throw new Error(`Perf user refresh input file has no data rows: ${csvPath}`);
  }

  const header = lines[0].split(',').map((h) => h.trim().toLowerCase());
  const usernameIdx = header.indexOf('username');
  if (usernameIdx < 0) {
    throw new Error(
      `Perf user refresh input must have a username column. Found: ${header.join(',')}`
    );
  }

  const usernames: string[] = [];
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(',').map((c) => c.trim());
    const username = cols[usernameIdx] ?? '';
    if (!username) {
      throw new Error(`Invalid row ${i + 1} in ${csvPath}: username is required`);
    }
    usernames.push(username);
  }

  if (usernames.length === 0) {
    throw new Error(`No username rows found in ${csvPath}`);
  }

  return usernames;
}

const perfUsernames = loadPerfUserRefreshUsernames(PERF_USER_REFRESH_CSV);

test.use({ storageState: undefined });

test.describe('Perf User Refresh - Login @perfUserRefresh', () => {
  test.beforeAll(() => {
    if (!process.env.MANAGE_CASE_BASE_URL) {
      throw new Error(
        'MANAGE_CASE_BASE_URL is not set. Set ENVIRONMENT to aat|demo|perftest|ithc ' +
          '(global-setup derives https://manage-case.<env>.platform.hmcts.net) or export MANAGE_CASE_BASE_URL.'
      );
    }
    if (!process.env.IDAM_PCS_USER_PASSWORD) {
      throw new Error(
        'IDAM_PCS_USER_PASSWORD is not set. Use the existing framework / Jenkins vault secret idam-pcs-user-password.'
      );
    }
  });

  for (const username of perfUsernames) {
    test(`Login [${username}]`, async ({ page, context }) => {
      await context.clearCookies();
      initializeExecutor(page);

      await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
      await dismissCookieBanner(page, 'additional');

      await performAction('login', {
        email: username,
        password: process.env.IDAM_PCS_USER_PASSWORD,
      });

      await dismissCookieBanner(page, 'analytics');

      // LoginAction already waits for Sign out; assert again for an explicit success check.
      await expect(page.getByText(home.signOutButton, { exact: true })).toBeVisible();
    });
  }
});
