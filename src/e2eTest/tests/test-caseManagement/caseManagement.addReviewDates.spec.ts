import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
import {initializeExecutor, performValidation} from '@utils/controller';
import test from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import {addReviewDates, checkYourAnswersAddReviewDates} from "@data/page-data-figma/page-data-caseManagement-figma";


test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');

  // for (const defendant of defendantUserDetails) {
  //   await performAction('makeAnApplicationAPI', {
  //     data: makeAnApplicationApiData.makeAnApplicationAdjournPayload(
  //       defendant.id,
  //       defendant.name
  //     ),
  //   });
  // };

  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.staffAdmin);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Case management - Case Worker Add Review date @nightly', async () => {
  test('Case management - Case Worker Add Review Date @CM @regression', async () => {
    await performAction('selectAnEvent', { eventType: caseSummary.addReviewDates });
    await performValidation('mainHeader', addReviewDates.mainHeader);
    performAction('clickButton', addReviewDates.addNewButton);
    await performAction('errorValidationAddReviewDatesPage', addReviewDates.errorValidation);
    await performAction('addReviewDates', {
      Day: addReviewDates.dayInputText,
      Month: addReviewDates.monthInputText,
      Year: addReviewDates.yearInputText,
      question: addReviewDates.reasonLabel, option: addReviewDates.dismissCaseRadioOption,
      label: addReviewDates.descriptionTextLabel, userInput: addReviewDates.descriptionTextInput,
      nextPage: checkYourAnswersAddReviewDates.mainHeader
    });
    await performAction('clickButton', checkYourAnswersAddReviewDates.submitButton);
    await performAction('confirmReviewDatesAdded');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Add review date');
  });
});
