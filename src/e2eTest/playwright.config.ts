import * as process from 'node:process';
import { defineConfig, devices, test as baseTest } from '@playwright/test';

const DEFAULT_VIEWPORT = { width: 1920, height: 1080 };

// Extend the base test to attach screenshots/videos on failure
export const test = baseTest.extend({
  page: async ({ page }, use, testInfo) => {
    await use(page);

    // Attach screenshot and video on failure
    if (testInfo.status !== 'passed') {
      // Attach screenshot if exists
      if (testInfo.attachments) {
        for (const attachment of testInfo.attachments) {
          if (attachment.name?.toLowerCase().includes('screenshot') && attachment.path) {
            testInfo.attachments.push({
              name: 'screenshot',
              path: attachment.path,
              contentType: 'image/png',
            });
          }

          if (attachment.name?.toLowerCase().includes('video') && attachment.path) {
            testInfo.attachments.push({
              name: 'video',
              path: attachment.path,
              contentType: 'video/webm',
            });
          }
        }
      }
    }
  },
});

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
    javaScriptEnabled: true,
    viewport: DEFAULT_VIEWPORT,
    headless: !!process.env.CI,
  },
  reporter: [
    ['list'],
    [
      'allure-playwright',
      {
        resultsDir: 'allure-results',
        suiteTitle: false,
        environmentInfo: { os_version: process.version },
      },
    ],
  ],
  projects: [
    {
      name: 'chrome',
      use: { ...devices['Desktop Chrome'] },
    },
    ...(process.env.CI
      ? [
        {
          name: 'firefox',
          use: { ...devices['Desktop Firefox'] },
        },
      ]
      : []),
  ],
});
