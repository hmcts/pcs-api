import * as process from 'node:process';
import { test as base } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { defineConfig, devices } from '@playwright/test';

const DEFAULT_VIEWPORT = { width: 1920, height: 1080 };

module.exports = defineConfig({
  testDir: 'tests/',
  /* Run tests in files in parallel */
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  timeout: 60 * 1000,
  expect: { timeout: 20 * 1000 },
  use: { actionTimeout: 30 * 1000, navigationTimeout: 20 * 1000 },
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
        detail: true,
        outputFolder: 'allure-results',
        categories: [
          {
            name: 'Failed tests',
            messageRegex: '.*AssertionError.*'
          }
        ]
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
        video: 'on',
        trace: 'on-first-retry',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
        headless: !!process.env.CI,
      },
    },
    ...(process.env.CI ? [
      {
        name: 'firefox',
        use: {
          ...devices["Desktop Firefox"],
          channel: 'firefox',
          screenshot: 'only-on-failure' as const,
          video: 'on' as const,
          trace: 'on-first-retry' as const,
          javaScriptEnabled: true,
          viewport: DEFAULT_VIEWPORT,
          headless: !!process.env.CI,
        }
      }
    ] : [])
  ]
});

export const test = base.extend({
  // Add afterEach to attach artifacts manually
  async page({ page }, use, testInfo) {
    await use(page);

    if (testInfo.status !== testInfo.expectedStatus) {
      // Copy screenshot
      const screenshotPath = path.join(testInfo.outputDir, `${testInfo.title.replace(/[/\\?%*:|"<>]/g, '_')}.png`);
      await page.screenshot({ path: screenshotPath, fullPage: true });

      // Copy video if it exists
      if (testInfo.attachments) {
        for (const attachment of testInfo.attachments) {
          if (attachment.name === 'video' && attachment.path) {
            const videoFile = path.basename(attachment.path);
            const allureResultsDir = path.join(process.cwd(), 'allure-results');
            if (!fs.existsSync(allureResultsDir)) fs.mkdirSync(allureResultsDir, { recursive: true });
            fs.copyFileSync(attachment.path, path.join(allureResultsDir, videoFile));
          }
        }
      }
    }
  },
});
