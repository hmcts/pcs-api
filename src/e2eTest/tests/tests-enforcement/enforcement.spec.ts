import { test } from "@playwright/test";
import { caseList } from "@data/page-data/caseList.page.data";
import { user } from "@data/user-data/permanent.user.data";
import { caseSummary } from "@data/page-data/caseSummary.page.data";
import { yourApplication } from "@data/page-data/page-data-enforcement/yourApplication.page.data";
import { initializeEnforcementExecutor, performAction, performValidation } from "@utils/controller-enforcement";
import { caseNumber } from "@utils/actions/custom-actions/createCase.action";
import { initializeExecutor } from "@utils/controller";
import { caseNotFoundAfterFilter } from "@utils/actions/custom-actions/searchCase.action";
import { nameAndAddressForEviction } from "@data/page-data/page-data-enforcement/nameAndAddressForEviction.page.data";
import { signInOrCreateAnAccount } from "@data/page-data/signInOrCreateAnAccount.page.data";

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAdditionalCookiesButton,
    hide: signInOrCreateAnAccount.hideThisCookieMessageButton
  });
  await performAction('login', user.claimantSolicitor);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAnalyticsCookiesButton
  });
  await performAction('filterCaseFromCaseList', caseList.stateAwaitingSubmission);
  await performAction('noCasesFoundAfterSearch')
  //Below three lines will be merged into a single action as part of improvement
  await performAction("selectFirstCaseFromTheFilter", caseNotFoundAfterFilter);
  await performAction('createNewCase', caseNotFoundAfterFilter);
  await performAction('searchMyCaseFromFindCase', { caseNumber: caseNumber, criteria: caseNotFoundAfterFilter });
});

test.describe('[Enforcement - Warrant of Possession] @Master @nightly', async () => {
  test('Apply for a Warrant of Possession ', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
    await performAction('selectApplicationType', { question: yourApplication.typeOfApplicationQuestion, option: yourApplication.typeOfApplicationOptions.warrantOfPossession });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
  });
});
