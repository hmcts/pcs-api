import { test } from '@fixtures/base.idam.fixture';
import {HomePage} from "@pages/home.page";

test('user sees Manage cases after login @functional @PR @nightly', async ({ loggedInPage },testInfo) => {
  await testInfo.attach('Page URL', {
    body: loggedInPage.url(),
    contentType: 'text/plain',
  });

  const homePage = new HomePage(loggedInPage);
  await homePage.expectManageCasesLinkVisible();
});
