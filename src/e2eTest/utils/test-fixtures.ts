import { test as base } from '@playwright/test';
import * as allure from 'allure-js-commons';
import { startLogCapture, getLogs } from '@utils/test-logger';

export const test = base.extend<{ _consoleLogCapture: void }>({
  _consoleLogCapture: [
      async ({ page }, use, testInfo) => {
      startLogCapture(page, testInfo);

      await use();

      const logs = getLogs(testInfo);
      if (logs.length > 0 && testInfo.status !== 'passed' && testInfo.status !== 'skipped') {
        await allure.step('Browser Console Logs', async () => {
          await allure.attachment('browser-console.log', logs.join('\n'), 'text/plain');
        });
      }
    },
    { auto: true },
  ],
});

export { expect } from '@playwright/test';
