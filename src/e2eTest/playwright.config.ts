import * as process from 'node:process';
import * as path from 'path';
import * as fs from 'fs';

import {defineConfig, devices} from '@playwright/test';

const DEFAULT_VIEWPORT = {width: 1920, height: 1080};
export const VERY_SHORT_TIMEOUT = 1000;
export const SHORT_TIMEOUT = 5000;
export const MEDIUM_TIMEOUT = 10000;
export const LONG_TIMEOUT = 30000;
export const actionRetries = 5;
export const waitForPageRedirectionTimeout = SHORT_TIMEOUT;

const SESSION_DIR = path.join(process.cwd(), '.auth');
const STORAGE_STATE_FILE = 'storage-state.json';
export const SESSION_COOKIE_NAME = 'Idam.Session';

export function getStorageStatePath(workerIndex?: number): string {
  const workerId = workerIndex ?? process.pid;
  const fileName = workerIndex !== undefined
    ? `storage-state-worker-${workerIndex}.json`
    : `storage-state-${workerId}.json`;
  return path.join(SESSION_DIR, fileName);
}

export function ensureWorkerStorageFile(): string {
  const masterPath = getMasterStorageStatePath();
  const workerPath = getStorageStatePath();
  const sessionDir = path.dirname(workerPath);

  if (!fs.existsSync(sessionDir)) {
    fs.mkdirSync(sessionDir, { recursive: true });
  }

  if (fs.existsSync(masterPath) && !fs.existsSync(workerPath)) {
    try {
      fs.copyFileSync(masterPath, workerPath);
    } catch {
      // Playwright will handle missing storage state gracefully
    }
  }

  return workerPath;
}

export function getMasterStorageStatePath(): string {
  return path.join(SESSION_DIR, STORAGE_STATE_FILE);
}

const getWorkers = () => {
  const env = process.env.ENVIRONMENT;
  return !env ? 1 : env === 'preview' ? 2 : env === 'aat' ? 4 : 4;
};

export default defineConfig({
  testDir: 'tests/',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 3 : 0,
  workers: getWorkers(),
  timeout: 600 * 1000,
  expect: { timeout: 30 * 1000 },
  use: {
    baseURL: process.env.MANAGE_CASE_BASE_URL || 'http://localhost:3000',
    actionTimeout: process.env.CI ? 60 * 1000 : 30 * 1000,
    navigationTimeout: process.env.CI ? 60 * 1000 : 30 * 1000,
    storageState: ensureWorkerStorageFile(),
  },
  reportSlowTests: { max: 15, threshold: 5 * 60 * 1000 },
  globalSetup: require.resolve('./config/global-setup.config'),
  globalTeardown: require.resolve('./config/global-teardown.config'),
  reporter: [
    ['list'],
    [
      'allure-playwright',
      {
        resultsDir: 'allure-results',
        suiteTitle: false,
        environmentInfo: {
          os_version: process.version,
          base_url: process.env.MANAGE_CASE_BASE_URL || 'http://localhost:3000',
          logged_in_user: process.env.IDAM_PCS_USER_EMAIL || 'pcs-solicitor-automation@test.com',
        },
      },
    ],
  ],
  projects: [
    {
      name: 'chrome',
      testMatch: /.*\.spec\.ts$/,
      testIgnore: /.*saveResume\.spec\.ts$/,
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        trace: 'on-first-retry',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
        headless: !!process.env.CI,
      },
    },
    {
      name: 'chrome-no-storage',
      testMatch: /.*saveResume\.spec\.ts$/,
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        trace: 'on-first-retry',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
        headless: !!process.env.CI,
        storageState: undefined,
      },
    },
    ...(process.env.CI ? [
      {
        name: 'firefox',
        testMatch: /.*\.spec\.ts$/,
        testIgnore: /.*saveResume\.spec\.ts$/,
        use: {
          ...devices["Desktop Firefox"],
          channel: 'firefox',
          screenshot: 'only-on-failure' as const,
          video: 'retain-on-failure' as const,
          trace: 'on-first-retry' as const,
          javaScriptEnabled: true,
          viewport: DEFAULT_VIEWPORT,
          headless: !!process.env.CI,
        }
      }
    ] : [])
  ]
});
