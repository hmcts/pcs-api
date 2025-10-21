import { test } from "@playwright/test";
import { caseList } from "@data/page-data/caseList.page.data";
import { user } from "@data/user-data/permanent.user.data";
import { caseSummary } from "@data/page-data/caseSummary.page.data";
import { yourApplication } from "@data/page-data/page-data-enforcement/yourApplication.page.data";
import { initializeEnforcementExecutor, performAction } from "@utils/controller-enforcement";
import { caseNumber } from "@utils/actions/custom-actions/createCase.action";
import { initializeExecutor } from "@utils/controller";
import { caseFoundAfterFilter } from "@utils/actions/custom-actions/searchCase.action";

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('login', user.claimantSolicitor);
  await performAction('filterCaseFromCaseList', caseList.stateAwaitingSubmission);
  await performAction('NoCasesFoundAfterSearch')
  //Below three lines will be merged into a single action as part of improvement 
  await performAction("selectFirstCaseFromTheFilter", caseFoundAfterFilter);
  await performAction('createNewCase',caseFoundAfterFilter);
  await performAction('searchMyCaseFromFindCase', { caseNumber: caseNumber, criteria: caseFoundAfterFilter });
});

test.describe.skip('[Enforcement - Warrant of Possession] @Master @nightly', async () => {
  test('Apply for a Warrant of Possession ', async () => {    
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
  });
});