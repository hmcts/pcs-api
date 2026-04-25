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

/** Allow only safe path fragments for E2E_SPEC glob segments (no slashes or glob metacharacters). */
function sanitizeSpecKeyword(k: string): string {
  return k.replace(/[^\w.-]/g, '');
}

/** Build test file globs from E2E_SPEC (comma or semicolon keywords). Empty = run all specs under testDir. */
function testMatchFromE2eSpec(raw: string | undefined): string[] | undefined {
  const keys = raw
    ?.split(/[,;]/)
    .map(k => sanitizeSpecKeyword(k.trim()))
    .filter(Boolean);
  return keys?.length ? keys.map(k => `**/*${k}*.spec.ts`) : undefined;
}

/** Title grep from E2E_TEST_SCOPE (nightly Jenkins or local). Invalid regex is escaped. */
function grepFromTestScope(raw: string): RegExp {
  try {
    return new RegExp(raw);
  } catch {
    return new RegExp(raw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
  }
}

const e2eSpecTestMatch = testMatchFromE2eSpec(process.env.E2E_SPEC);
const rawScope = process.env.E2E_TEST_SCOPE;
const e2eGrep =
  rawScope !== undefined && rawScope !== '' ? grepFromTestScope(rawScope) : undefined;

export default defineConfig({
  testDir: 'tests/',
  ...(e2eSpecTestMatch?.length ? { testMatch: e2eSpecTestMatch } : {}),
  ...(e2eGrep ? { grep: e2eGrep } : {}),
  /* Run tests in files in parallel */
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
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
