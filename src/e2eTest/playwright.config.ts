import * as process from 'node:process';
import * as path from 'path';
import * as fs from 'fs';

import {defineConfig, devices} from '@playwright/test';

const DEFAULT_VIEWPORT = {width: 1920, height: 1080};
export const VERY_SHORT_TIMEOUT = 1000;
export const SHORT_TIMEOUT = 5000;
export const MEDIUM_TIMEOUT = 10000;
export const LONG_TIMEOUT = 30000;
export const VERY_LONG_TIMEOUT = 60000;
export const actionRetries = 5;
export const waitForPageRedirectionTimeout = SHORT_TIMEOUT;
const STORAGE_STATE_PATH = path.join(__dirname, '.auth/storage-state.json');
const storageStateConfig = fs.existsSync(STORAGE_STATE_PATH) ? { storageState: STORAGE_STATE_PATH } : {};
const e2eTag = process.env.E2E_TEST_SCOPE ?? '@nightly';

export default defineConfig({
  testDir: 'tests/',
  ...(e2eTag ? { grep: new RegExp(e2eTag) } : {}),
  /* Run tests in files in parallel */
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 1 : 0,
  //Configure workers by environment: AAT is fixed at 4 workers; preview worker count can be adjusted based on preview performance
  workers: process.env.ENVIRONMENT === 'preview' ? 1 : 4,
  timeout: 600 * 1000,
  expect: { timeout: 30 * 1000 },
  use: { actionTimeout: 40 * 1000,  navigationTimeout: 40 * 1000, ...storageStateConfig },
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
      },
    },
    ...(process.env.CI
      ? [
          {
            name: 'firefox',
            use: {
              ...devices['Desktop Firefox'],
              channel: 'firefox',
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              viewport: DEFAULT_VIEWPORT,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'webkit',
            use: {
              ...devices['Desktop Safari'],
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              viewport: DEFAULT_VIEWPORT,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'edge',
            use: {
              ...devices['Desktop Edge'],
              channel: 'msedge',
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              viewport: DEFAULT_VIEWPORT,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'mobile-android',
            use: {
              ...devices['Pixel 5'],
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'mobile-ios',
            use: {
              ...devices['iPhone 12'],
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'mobile-ipad',
            use: {
              ...devices['iPad Pro 11'],
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              headless: !!process.env.CI,
            },
          },
        ]
      : []),
  ],
});
