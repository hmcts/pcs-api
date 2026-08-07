import {createCaseApiData, submitCaseApiData} from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';
import { addHearing, checkYourAnswersManageHearing, manageHearing } from '@data/page-data-figma/page-data-caseManagement-figma';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';



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

test.describe('Case management - Case Worker Manage Hearing @nightly', async () => {
  test('Case management - Case Worker Cancel a hearing @CM @regression', async () => {
    await performAction('selectAnEvent', {eventType: caseSummary.manageHearing});
  })

  test('Case management - Case Worker Add a hearing @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(addHearing.dateTypeHiddenUserInput,'dateTime');
    let typeOfHearing = addHearing.typeOfHearingOption[0]
    await performAction('selectAnEvent', {eventType: caseSummary.manageHearing});
    await performValidation('mainHeader', addHearing.mainHeader);
    await performAction('addAHearing', {
      question: addHearing.typeOfHearingQuestion, option: typeOfHearing,
      question1: addHearing.wordingForHearingNoticeTextLabel, option1: addHearing.wordingForHearingHiddenOption,
      label1: addHearing.whenIsTheHearingQuestion,
      date: date,
      label2: addHearing.hourTextLabel,
      input2: CaseManagementCommonUtils.getRandomNumberAsString(1, 5),
      label3: addHearing.minutesTextLabel,
      input3: CaseManagementCommonUtils.getRandomNumberAsString(1, 60),
      question2: addHearing.hearingNoticeQuestion, option2: addHearing.hearingNoticeNoRadioOption,
      nextPage: checkYourAnswersManageHearing.mainHeader
    })
  })
});