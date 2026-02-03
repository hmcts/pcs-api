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
        await allure.parameter('BrowserConsoleLogs', 'See attachments under After Hooks → Fixture "_consoleLogCapture"');
        await allure.step('Browser Console Logs', async () => {
        });
      }
    },
    { auto: true },
  ],
});

export { expect } from '@playwright/test';
