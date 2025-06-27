import { startCaseCreation,selectAddress,enterApplicantDetails,submitCaseCreation } from '../commonSteps/caseCreation';
import * as idamHelper from "@helpers/idam-helpers/idam.helper";
import { test, Username } from '@fixtures/login.fixture';
import * as allure from 'allure-js-commons';

test.beforeAll(async () => {
  await allure.parentSuite('Create Case');
});


test.describe.skip('Create Case with Address @Master @nightly', () => {

  test('should create a case successfully with Dropdown Address Flow', async ({ loggedInPage }, testInfo) => {
    await testInfo.attach('Page URL', {
      body: loggedInPage.url(),
      contentType: 'text/plain',
    });
    await startCaseCreation(loggedInPage);
    await selectAddress(loggedInPage, false);
    await enterApplicantDetails(loggedInPage);
    await submitCaseCreation(loggedInPage);
  });

  test('should create a case successfully with Manual Address Flow', async ({ loggedInPage },testInfo) => {
    await testInfo.attach('Page URL', {
      body: loggedInPage.url(),
      contentType: 'text/plain',
    });
    await startCaseCreation(loggedInPage);
    await selectAddress(loggedInPage, true);
    await enterApplicantDetails(loggedInPage);
    await submitCaseCreation(loggedInPage);
  });
  test.afterAll(async () => {
    if (Username) {
      try {
        await idamHelper.deleteAccount(Username);
      } catch (err) {
        console.warn(`Teardown failed for user ${Username}:`, err);
      }
    }
  });
});
