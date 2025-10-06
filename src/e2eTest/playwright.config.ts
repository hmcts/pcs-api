import { defineConfig, devices, TestInfo } from '@playwright/test';
import * as process from 'node:process';
import fs from 'fs';

const DEFAULT_VIEWPORT = { width: 1920, height: 1080 };

// Global afterEach hook to attach last failed retry's screenshot and video
export const globalHooks = {
  async afterEach({ }, testInfo: TestInfo) {
    if (testInfo.status !== 'passed') {
      // Attach screenshot if exists
      const screenshot = testInfo.attachments.find(a => a.name === 'screenshot')?.path;
      if (screenshot && fs.existsSync(screenshot)) {
        await testInfo.attach('Last failed screenshot', {
          path: screenshot,
          contentType: 'image/png',
        });
      }

      // Attach video if exists
      const video = testInfo.attachments.find(a => a.name === 'video')?.path;
      if (video && fs.existsSync(video)) {
        await testInfo.attach('Last failed video', {
          path: video,
          contentType: 'video/webm',
        });
      }
    }
  }
};

export default defineConfig({
  testDir: 'tests/',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  timeout: 60 * 1000,
  expect: { timeout: 20 * 1000 },
  use: {
    actionTimeout: 30 * 1000,
    navigationTimeout: 20 * 1000,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'on-first-retry',
    viewport: DEFAULT_VIEWPORT,
    javaScriptEnabled: true,
    headless: !!process.env.CI,
  },
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
  globalSetup: require.resolve('./config/global-setup.config'),
  globalTeardown: require.resolve('./config/global-teardown.config'),
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
  // Attach global hooks here
  ...globalHooks,
});
