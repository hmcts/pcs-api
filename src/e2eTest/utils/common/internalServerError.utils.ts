import type { Page } from '@playwright/test';

export function collectBrowserErrors(page: Page) {
  const errors: string[] = [];

  page.on('response', response => {
    if (response.status() >= 500) {
      errors.push(
        `HTTP ${response.status()} - ${response.url()}`
      );
    }
  });

  page.on('requestfailed', request => {
    errors.push(
      `Request failed: ${request.url()} - ${
        request.failure()?.errorText ?? 'Unknown error'
      }`
    );
  });

  page.on('pageerror', error => {
    errors.push(`Page error: ${error.message}`);
  });

  page.on('console', message => {
    if (message.type() !== 'error') {
      return;
    }

    errors.push(`Console error: ${message.text()}`);
  });

  return {
    getErrors: () => [...errors],

    assertNoErrors: () => {
      if (errors.length > 0) {
        throw new Error(
          `Browser errors detected:\n\n${errors.join('\n')}`
        );
      }
    },
  };
}