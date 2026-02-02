import { test as base } from '@playwright/test';
import { startLogCapture, attachLogToTest } from '@utils/test-logger';

export const test = base.extend<{ _consoleLogCapture: void }>({
  _consoleLogCapture: [
    async ({ page }, use) => {
      await use();
    },
    { auto: true },
  ],
});

test.beforeEach(async ({ page }, testInfo) => {
  startLogCapture(page, testInfo);
});

test.afterEach(async ({}, testInfo) => {
  await attachLogToTest(testInfo);
});

export { expect } from '@playwright/test';
