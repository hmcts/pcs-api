import {test} from "@playwright/test";
import {caseList} from "@data/page-data/page-data-enforcement/caseList.page.data";
import {user} from "@data/user-data/permanent.user.data";
import {parentSuite} from "allure-js-commons";
import {caseSummary} from "@data/page-data/page-data-enforcement/caseSummary.page.data";
import { yourApplication } from "@data/page-data/page-data-enforcement/yourApplication.page.data";
import { initializeExecutor, performAction } from "@utils/controller-enforcement";


test.beforeEach(async ({page}, testInfo) => {
  initializeExecutor(page);
  //await parentSuite('Case Creation');
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('loginEnforcement', user.claimantSolicitor);
  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
  await performAction('caseFilter',caseList.state);
});

test.describe('[Enforcement  @Master @nightly', async () => {
   test('Enforcement apply for Warrant ', async () => {
    await performAction('select', caseSummary.NextStepLabel,caseSummary.selectEvent);
    await performAction('clickButtonAndVerifyPageNavigation',caseSummary.go,yourApplication.mainHeader);
  });
});