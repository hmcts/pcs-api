import type { Page } from '@playwright/test';
import type { TestInfo } from '@playwright/test';

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

export async function attachLogToTest(testInfo: TestInfo): Promise<void> {
  const logs = logCaptureByTestId.get(testInfo.testId);
  logCaptureByTestId.delete(testInfo.testId);

  if (!logs || logs.length === 0) return;

  const failed = testInfo.status !== 'passed' && testInfo.status !== 'skipped';
  if (!failed) return;

  await testInfo.attach('Browser Console Logs', {
    body: logs.join('\n'),
    contentType: 'text/plain',
  });
}
