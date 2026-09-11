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

// Nightly (Jenkins): E2E_TEST_SCOPE = tag grep; E2E_SPEC = comma/semicolon path keywords → testMatch globs.
const e2eSpecKeys = (process.env.E2E_SPEC ?? '')
  .split(/[,;]/)
  .map(s => s.trim().replace(/[^\w.-]/g, ''))
  .filter(Boolean);
const e2eTestMatch = e2eSpecKeys.length ? e2eSpecKeys.map(k => `**/*${k}*.spec.ts`) : undefined;
const e2eScope = process.env.E2E_TEST_SCOPE?.trim();
const e2eGrep = e2eScope ? new RegExp(e2eScope) : undefined;

// Preview matches AAT at 4. It previously sat at 2, and raising it alone was measured as worse
// (3 workers: 6 flaky / 18.6m against 0 flaky / 11.8-13.2m at 2) because ccd-data-store's JDBC pool
// saturated at its ceiling of 5 with seven requests queued, each waiting the full 40s. That ceiling
// is raised alongside this in values.ccd.preview.template.yaml; the two only work together.
// Use E2E_WORKERS to tune without a code change.
function resolveWorkers(): number {
  const environmentDefault = 4;
  const parsed = Number(process.env.E2E_WORKERS?.trim());
  return Number.isInteger(parsed) && parsed >= 1 ? parsed : environmentDefault;
}

export default defineConfig({
  testDir: 'tests/',
  ...(e2eTestMatch?.length ? { testMatch: e2eTestMatch } : {}),
  ...(e2eGrep ? { grep: e2eGrep } : {}),
  /* Run tests in files in parallel */
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  workers: resolveWorkers(),
  /* Inside e2e-output so Jenkins retains traces and videos with the E2E report */
  outputDir: 'e2e-output/test-results',
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
