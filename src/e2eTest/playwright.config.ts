import * as process from 'node:process';

import { defineConfig, devices } from '@playwright/test';

const DEFAULT_VIEWPORT = { width: 1920, height: 1080 };

module.exports = defineConfig({
  testDir: 'tests/',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  timeout: 6 * 60 * 1000,
  expect: { timeout: 60_000 },
  /* Report slow tests if they take longer than 5 mins */
  reportSlowTests: { max: 15, threshold: 5 * 60 * 1000 },
  workers: process.env.FUNCTIONAL_TESTS_WORKERS ? parseInt(process.env.FUNCTIONAL_TESTS_WORKERS) : 4,
  reporter: process.env.CI ? [['html'], ['list']] : [['list']],
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chromium',
        screenshot: 'off',
        video: 'off',
        trace: 'on-first-retry',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
      },
    },
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
        screenshot: 'off',
        video: 'off',
        trace: 'on-first-retry',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
      },
    },
    {
      name: 'webkit',
      use: {
        ...devices['Desktop Safari'],
        screenshot: 'off',
        video: 'off',
        trace: 'on-first-retry',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
      },
    },
    {
      name: 'MobileChrome',
      use: {
        ...devices['Pixel 5'],
        trace: 'on-first-retry',
      },
    },
    {
      name: 'MobileSafari',
      use: {
        ...devices['iPhone 12'],
        trace: 'on-first-retry',
      },
    },
    {
      name: 'MicrosoftEdge',
      use: {
        ...devices['Desktop Edge'],
        channel: 'msedge',
        trace: 'on-first-retry',
      },
    },
  ],
});
