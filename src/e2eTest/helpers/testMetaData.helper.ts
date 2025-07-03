import type { Page, TestInfo } from '@playwright/test';

export async function attachTestMetadata(testInfo: TestInfo, page: Page, email: string) {
  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });

  await testInfo.attach('Email Address', {
    body: email,
    contentType: 'text/plain',
  });
}
