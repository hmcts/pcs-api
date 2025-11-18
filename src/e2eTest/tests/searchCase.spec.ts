import { test } from '@playwright/test';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { caseInfo } from '@utils/actions/custom-actions';
import { user } from '@data/user-data';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { signInOrCreateAnAccount } from '@data/page-data';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await performAction('createCase', { data: createCaseApiData.createCasePayload });
  await performAction('submitCase', { data: submitCaseApiData.submitCasePayload });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAdditionalCookiesButton,
    hide: signInOrCreateAnAccount.hideThisCookieMessageButton
  });
  await performAction('login', user.claimantSolicitor);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAnalyticsCookiesButton
  });
});

//Skipping these tests until create case journey is fully developed because tests may fail each time when payload changes for create case API
test.describe('[Search Case] @PR @Master @nightly', () => {
  test.skip('Search for case via case list', async ({}) => {
    await performAction('filterCaseFromCaseList', caseInfo.state);
    await performValidation('text', {
      "text": 'Case number: '+caseInfo.fid,
      "elementType": "paragraph"
    });
  });

  test('Search for case via find case', async ({}) => {
    await performAction('searchCaseFromFindCase', caseInfo.id);
    await performValidation('text', {
      "text": 'Case number: '+caseInfo.fid,
      "elementType": "paragraph"
    });
  });
});
