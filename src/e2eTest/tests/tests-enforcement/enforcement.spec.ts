import { test } from "@playwright/test";
import { caseList } from "@data/page-data/page-data-enforcement/caseList.page.data";
import { user } from "@data/user-data/permanent.user.data";
import { caseSummary } from "@data/page-data/page-data-enforcement/caseSummary.page.data";
import { yourApplication } from "@data/page-data/page-data-enforcement/yourApplication.page.data";
import { initializeExecutor, performAction, performValidation } from "@utils/controller-enforcement";
import { noCaseFound, randomCaseNumber } from "@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action";
import { caseNumber } from "@utils/actions/custom-actions/createCase.action";


let testCaseNumber: string;
test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('loginEnforcement', user.claimantSolicitor);
  await performAction('caseFilter', caseList.state);
  await performAction('yourCases')
  if (noCaseFound == false) {
    await performAction("pickAnyCase");
  } else {
    await performAction('makeClaim');
    await performAction('findCase', caseNumber);
  }
  testCaseNumber = noCaseFound == false ? randomCaseNumber : caseNumber;
});

test.describe('[Enforcement  @Master @nightly', async () => {
  test('Enforcement apply for Warrant ', async () => {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + testCaseNumber });
    await performAction('select', caseSummary.NextStepLabel, caseSummary.selectEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
    await performValidation('mainHeader', yourApplication.mainHeader);
  });
});