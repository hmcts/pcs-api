import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';
import { enterGenappApplication } from '@data/page-data-figma/page-data-caseManagement-figma';

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  allPartyDetails.length = 0;
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('getAllPartyDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseFileView.defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayloadCaseFileView.addAnotherDefendant,
    payLoad: submitCaseApiData.submitCasePayloadCaseFileView
  });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.hearingCenterAdmin);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Case management - Case Worker Enter a General application @nightly', async () => {
  test('Case management - Case Worker Enter a General application ADJOURN Journey @CM @regression', async () => {
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('errorValidationEnterGeneralAppPage', enterGenappApplication.errorValidation);
    // await performAction('changeCaseState', {
    //   question: changeCaseState.whichStateYouMovingCaseToQuestion, option: changeCaseState.caseStateHiddenOption,
    //   nextPage: checkYourAnswersChangeState.mainHeader
    // });
    // await performAction('clickButton', checkYourAnswersChangeState.submitButton);
    // await performAction('confirmCaseStateChange');
    // await performValidation('bannerAlert', 'Case #.* has been updated with event: Change case state');
  });
});
