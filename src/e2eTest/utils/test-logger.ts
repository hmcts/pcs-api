import type { Page } from '@playwright/test';
import type { TestInfo } from '@playwright/test';
import * as allure from 'allure-js-commons';
import { ContentType } from 'allure-js-commons';

const logCaptureByTestId = new Map<string, string[]>();

export function startLogCapture(page: Page, testInfo: TestInfo): void {
  const logs: string[] = [];
  const testId = testInfo.testId;

  const handler = (msg: { type: () => string; text: () => string }) => {
    const timestamp = new Date().toISOString();
    logs.push(`[${timestamp}] [${msg.type()}] ${msg.text()}`);
  };

  page.on('console', handler);
  logCaptureByTestId.set(testId, logs);
}

export async function logToBrowser(page: Page, message: string): Promise<void> {
  try {
    await page.evaluate((msg) => {console.log(`[E2E] ${msg}`);}, message);
  } catch (err) {
    console.warn('[E2E] Could not log to browser console (page may be destroyed or navigating):', (err as Error).message);
  }
}

export async function attachLogToTest(testInfo: TestInfo): Promise<void> {
  const failed = testInfo.status !== 'passed' && testInfo.status !== 'skipped';
  if (!failed) return;

  const logs = logCaptureByTestId.get(testInfo.testId) ?? [];
  logCaptureByTestId.delete(testInfo.testId);

  const content =
    logs.length > 0
      ? logs.join('\n')
      : 'No browser console output was captured. This can happen if the test failed before page load, the page was destroyed early, or the test retried in a new worker (logs from the previous attempt are not carried over).';

  await allure.parameter('Browser logs', 'See "Browser console logs" attachment under After Hooks at the end of the report', { excluded: true });
  await allure.step('Browser console logs', async () => {
    await allure.attachment('Browser console logs', content, ContentType.TEXT);
  });
}
