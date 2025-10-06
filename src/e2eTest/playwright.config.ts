import * as process from 'node:process';
import { defineConfig, devices } from '@playwright/test';

const DEFAULT_VIEWPORT = { width: 1920, height: 1080 };

module.exports = defineConfig({
  testDir: 'tests/',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,  // retry on CI
  timeout: 60 * 1000,
  expect: { timeout: 20 * 1000 },
  use: {
    actionTimeout: 30 * 1000,
    navigationTimeout: 20 * 1000,
    screenshot: 'only-on-failure',       // capture screenshot on any failed attempt
    video: 'retain-on-failure',          // capture video on any failed attempt
    trace: 'on-first-retry',             // keep trace only on first retry
    headless: !!process.env.CI,
    viewport: DEFAULT_VIEWPORT,
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
        // this ensures attachments from all failed retries are included
        attachments: { includeAllRetries: true },
      },
    ],
  ],
  projects: [
    {
      name: 'chrome',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
      },
    },
    ...(process.env.CI
      ? [
        {
          name: 'firefox',
          use: {
            ...devices['Desktop Firefox'],
            channel: 'firefox',
          },
        },
      ]
      : []),
  ],
});
