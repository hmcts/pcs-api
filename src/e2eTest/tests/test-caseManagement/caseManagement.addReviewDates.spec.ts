import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performValidation } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import { addReviewDates, checkYourAnswersAddReviewDates } from "@data/page-data-figma/page-data-caseManagement-figma";
import { formatDate, generateRandomString, getCurrentBSTTime } from '@utils/common/string.utils';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';


test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await test.step(`Clear all cookies and initialize the test executors`, async () => {
    await context.clearCookies();
    initializeExecutor(page);
    initializeCMExecutor(page);
  });

  await test.step('Create and submit case, update payment and retrieve party details', async () => {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView() });
    await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
    await performAction('updatePaymentAPI');
    await performAction('getCaseAPI', 'Link Solicitor');
    await performAction('fetchCurrentUserAPI', 'CaseWorker');
  });

  await test.step(`Login as Hearing Center Admin - ${user.hearingCenterAdmin.email} and navigate to Case Summary page`, async () => {
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await dismissCookieBanner(page, 'additional');
    await performAction('login', user.hearingCenterAdmin);
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToSummaryPage');
  });
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Case management - Case Worker Add Review date @nightly', async () => {
  test('Case management - As a Case Worker I want to successfully Add Review Dates',
    {
      tag: ['@CM', '@regression'],
      annotation: [
        { type: 'jira', description: `Updates to this tests were made as part of : https://hmcts.atlassian.net/browse/HDPI-8351` },
        { type: 'ac', description: 'AC2' },
      ],
    }, async () => {
      const userInput =
        typeof addReviewDates.descriptionTextInput === 'number'
          ? generateRandomString(addReviewDates.descriptionTextInput)
          : (addReviewDates.descriptionTextInput as string);
      let date = CaseManagementCommonUtils.getRandomDate(addReviewDates.dateTypeHiddenUserInput as string);
      let reviewReason = addReviewDates.reviewReasonArray[Math.floor(Math.random() * addReviewDates.reviewReasonArray.length)];
      let currentTime: string = '';

      await test.step(`Given the user has selected the Add review date event`, async () => {
        await performAction('selectAnEvent', { eventType: caseSummary.addReviewDates });
        await performValidation('mainHeader', addReviewDates.mainHeader);
      });

      await test.step(`Then the user performs all error message validation on Review dates page with different sets of data`, async () => {
        await performAction('clickButton', addReviewDates.addNewButton);
        await performAction('errorValidationAddReviewDatesPage', addReviewDates.errorValidation);
      });

      await test.step(`When the user enters valid Review Dates`, async () => {
        await performAction('addReviewDates', {
          reviewDateLabel: addReviewDates.dateOfReviewHiddenLabel,
          date: date,
          question: addReviewDates.reasonHiddenLabel, option: reviewReason,
          label: addReviewDates.descriptionHiddenTextLabel, userInput: userInput,
          nextPage: checkYourAnswersAddReviewDates.mainHeader
        });
      });

      await test.step(`And submits the new Review date`, async () => {
        await performAction('clickButton', checkYourAnswersAddReviewDates.submitButton)
      });

      await test.step(`Then the confirmation page displays the details of the new Review Date details`, async () => {
        await performAction('confirmReviewDatesAdded');
        currentTime = getCurrentBSTTime();
        await performValidation('bannerAlert', 'Case #.* has been updated with event: Add review date');
      })

      await test.step(`And the new Review date added is displayed in the Case Notes Section`, async () => {
        await performAction('clickTab', home.caseNotes);
        await performAction('validateCaseNotesDetails', {
          createdDate: currentTime.replace(/:\d{2} /, " "),
          reviewDescription: userInput,
          reviewDate: formatDate(date, 'DD/MON/YYYY'),
          reviewReason: reviewReason,
          table: 'Review date 1'
        });
      });
    });
});
