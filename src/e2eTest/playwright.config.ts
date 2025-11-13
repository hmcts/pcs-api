import * as process from 'node:process';
import * as fs from 'fs';
import * as path from 'path';

import {defineConfig, devices} from '@playwright/test';

const DEFAULT_VIEWPORT = {width: 1920, height: 1080};
export const VERY_SHORT_TIMEOUT = 1000;
export const SHORT_TIMEOUT = 5000;
export const MEDIUM_TIMEOUT = 10000;
export const LONG_TIMEOUT = 30000;
export const actionRetries = 5;
export const waitForPageRedirectionTimeout = SHORT_TIMEOUT;

// Storage state path (global-setup creates it)
// Always provide the path - Playwright will check if file exists when creating contexts
const storageStatePath = path.join(process.cwd(), '.auth', 'storage-state.json');

export default defineConfig({
  testDir: 'tests/',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 3 : 0,
  // Reduced workers from 4 → 2 due to server/login contention issues
  workers: 1,
  timeout: 600 * 1000,
  expect: { timeout: 30 * 1000 },
  use: {
    baseURL: process.env.MANAGE_CASE_BASE_URL || 'http://localhost:3000',
    actionTimeout: process.env.CI ? 60 * 1000 : 30 * 1000,
    navigationTimeout: process.env.CI ? 60 * 1000 : 30 * 1000,
    storageState: storageStatePath
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
        storageState: storageStatePath,
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
          storageState: storageStatePath,
        }
      }
    ] : [])
  ]
});
