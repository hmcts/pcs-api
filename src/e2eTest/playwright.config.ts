import * as process from 'node:process';
import * as fs from 'fs';

import {defineConfig, devices} from '@playwright/test';
import { SessionManager } from '@utils/session-manager';

const DEFAULT_VIEWPORT = {width: 1920, height: 1080};
export const VERY_SHORT_TIMEOUT = 1000;
export const SHORT_TIMEOUT = 5000;
export const MEDIUM_TIMEOUT = 10000;
export const LONG_TIMEOUT = 30000;
export const actionRetries = 5;
export const waitForPageRedirectionTimeout = SHORT_TIMEOUT;

// Check if storage state exists for session reuse
const storageStatePath = SessionManager.getStorageStatePath();
const storageState = fs.existsSync(storageStatePath) ? storageStatePath : undefined;

export default defineConfig({
  testDir: 'tests/',
  /* Run tests in files in parallel */
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 3 : 0,
  // Reduced workers from 4 → 2 due to server/login contention issues
  workers: 4,
  timeout: 600 * 1000,
  expect: { timeout: 30 * 1000 },
  use: {
    baseURL: process.env.MANAGE_CASE_BASE_URL || 'http://localhost:3000',
    actionTimeout: 30 * 1000,
    navigationTimeout: 30 * 1000,
    // Use storage state if available (will be created by globalSetup)
    ...(storageState ? { storageState } : {})
  },
  /* Report slow tests if they take longer than 5 mins */
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
        },
      },
    ],
  ],
  projects: [
    {
      name: 'chrome',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        trace: 'on-first-retry',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
        headless: !!process.env.CI,
        // Use the authenticated storage state (created by globalSetup)
        ...(storageState ? { storageState } : {}),
      },
    },
    ...(process.env.CI ? [
      {
        name: 'firefox',
        use: {
          ...devices["Desktop Firefox"],
          channel: 'firefox',
          screenshot: 'only-on-failure' as const,
          video: 'retain-on-failure' as const,
          trace: 'on-first-retry' as const,
          javaScriptEnabled: true,
          viewport: DEFAULT_VIEWPORT,
          headless: !!process.env.CI,
          // Use the authenticated storage state (created by globalSetup)
          ...(storageState ? { storageState } : {}),
        }
      }
    ] : [])
  ]
});
