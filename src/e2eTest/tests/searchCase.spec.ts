import {test} from '@playwright/test';
import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import {caseInfo} from '@utils/actions/custom-actions/createCase.action';
import {user} from '@data/user-data/permanent.user.data';
import {signInOrCreateAnAccount} from "@data/page-data/signInOrCreateAnAccount.page.data";
import {home} from "@data/page-data/home.page.data";

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('handleCookieConsent', {
    accept:signInOrCreateAnAccount.acceptAdditionalCookies,
    hide:signInOrCreateAnAccount.hideThisCookieMessage
  });
  await performAction('login', user.claimantSolicitor);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAnalyticsCookies
  });
  await performAction('clickTab', home.createCaseTab);
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

//Skipping these tests until create case journey is fully developed because tests may fail each time when payload changes for create case API
test.describe.skip('[Search Case] @PR @Master @nightly', () => {
  test('Search for case via caselist', async ({}) => {
    await performAction('searchCaseFromCaseList', caseInfo.id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: caseInfo.fid}
    );
  });
  test('Search for case via find case', async ({}) => {
    await performAction('searchCaseFromCaseList', caseInfo.id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: caseInfo.fid}
    );
  });
});
