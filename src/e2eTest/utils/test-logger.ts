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

export function getLogs(testInfo: TestInfo): string[] {
  return logCaptureByTestId.get(testInfo.testId) ?? [];
}
