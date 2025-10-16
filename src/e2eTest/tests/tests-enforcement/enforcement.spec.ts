import { test } from "@playwright/test";
import { caseList } from "@data/page-data/caseList.page.data";
import { user } from "@data/user-data/permanent.user.data";
import { caseSummary } from "@data/page-data/caseSummary.page.data";
import { yourApplication } from "@data/page-data/page-data-enforcement/yourApplication.page.data";
import { initializeEnforcementExecutor, performAction } from "@utils/controller-enforcement";
import { caseNumber } from "@utils/actions/custom-actions/createCase.action";
import { initializeExecutor } from "@utils/controller";
import { firstFromTheListCaseNumber, searchReturnFromFilter } from "@utils/actions/custom-actions/searchCase.action";

let testCaseNumber: string;

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('login', user.claimantSolicitor);
  await performAction('filterCaseFromCaseList', caseList.stateAwaitingSubmission);
  await performAction('NoCasesFoundAfterSearch')
  if (searchReturnFromFilter == false) {
    await performAction("selectFirstCaseFromTheFilter");
  } else {
    await performAction('createNewCase');
    await performAction('searchMyCaseFromFindCase', caseNumber);
  }
  testCaseNumber = searchReturnFromFilter == false ? firstFromTheListCaseNumber : caseNumber;
});

test.describe('[Enforcement  @Master @nightly', async () => {
  test('Enforcement apply for Warrant ', async () => {    
    await performAction('select', caseSummary.nextStepLabel, caseSummary.selectEnforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
  });
});