import { test, expect } from '@fixtures/base.idam.fixture';

test('user sees Manage cases after login @functional @PR @nightly', async ({ loggedInPage },testInfo) => {
  await testInfo.attach('Page URL', {
    body: loggedInPage.url(),
    contentType: 'text/plain',
  });
  const manageCasesLink = loggedInPage.locator('a', { hasText: 'Manage Cases' });
  await expect(manageCasesLink).toBeVisible();
});
