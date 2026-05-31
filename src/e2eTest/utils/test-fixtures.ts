import { test as base } from '@playwright/test';
import { startLogCapture, attachLogToTest } from '@utils/test-logger';

export const test = base.extend<{ _consoleLogCapture: void }>({
  _consoleLogCapture: [
    async ({ page }, use, testInfo) => {
      startLogCapture(page, testInfo);
      await use();
      await attachLogToTest(testInfo);
    },
    { auto: true },
  ],
});

export { expect } from '@playwright/test';
