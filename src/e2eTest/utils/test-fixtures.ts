import { test as base } from '@playwright/test';
import { startLogCapture, attachLogToTest } from '@utils/test-logger';
import { enableWelshCapture, WELSH_CAPTURE_ENABLED } from '@utils/welsh-capture';

export const test = base.extend<{ _consoleLogCapture: void; _welshCapture: void }>({
  _consoleLogCapture: [
    async ({ page }, use, testInfo) => {
      startLogCapture(page, testInfo);
      await use();
      await attachLogToTest(testInfo);
    },
    { auto: true },
  ],
  // Opt-in with WELSH_CAPTURE=1: runs the test in Welsh mode, keeps the UI English, records which
  // phrases ExUI asked to translate and whether AAT has a Welsh translation. See utils/welsh-capture.ts.
  _welshCapture: [
    async ({ context }, use, testInfo) => {
      if (!WELSH_CAPTURE_ENABLED) {
        await use();
        return;
      }
      const flush = await enableWelshCapture(context, testInfo);
      await use();
      await flush();
    },
    { auto: true },
  ],
});

export { expect } from '@playwright/test';
