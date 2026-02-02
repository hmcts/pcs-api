import type { Page } from '@playwright/test';
import type { TestInfo } from '@playwright/test';
import * as allure from 'allure-js-commons';
import { ContentType } from 'allure-js-commons';

interface LogEntry {
  type: string;
  timestamp: string;
  text: string;
}

const logCaptureByTestId = new Map<string, LogEntry[]>();

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

export function startLogCapture(page: Page, testInfo: TestInfo): void {
  const logs: LogEntry[] = [];
  const testId = testInfo.testId;

  const handler = (msg: { type: () => string; text: () => string }) => {
    logs.push({
      type: msg.type(),
      timestamp: new Date().toISOString(),
      text: msg.text(),
    });
  };

  page.on('console', handler);
  logCaptureByTestId.set(testId, logs);
}

export async function attachLogToTest(testInfo: TestInfo): Promise<void> {
  const logs = logCaptureByTestId.get(testInfo.testId);
  logCaptureByTestId.delete(testInfo.testId);

  if (!logs || logs.length === 0) {
    return;
  }

  const failed = testInfo.status !== 'passed' && testInfo.status !== 'skipped';
  if (!failed) {
    return;
  }

  const lines = logs.map((e) => {
    const line = `[${e.timestamp}] [${e.type}] ${escapeHtml(e.text)}`;
    if (e.type === 'error') {
      return `<span style="color:#c00;font-weight:bold">${line}</span>`;
    }
    if (e.type === 'warning') {
      return `<span style="color:#c60">${line}</span>`;
    }
    return line;
  });

  const html = `<pre style="font-family:monospace;font-size:12px;white-space:pre-wrap;margin:0">${lines.join('\n')}</pre>`;
  await allure.attachment('Browser console log', html, ContentType.HTML);
}
