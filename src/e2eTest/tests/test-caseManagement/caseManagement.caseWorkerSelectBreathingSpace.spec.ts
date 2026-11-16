import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';
import { enterBreathingSpace, enterBreathingSpacecheckYourAnswers } from '@data/page-data-figma/page-data-caseManagement-figma';
import { label } from 'allure-js-commons';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  allPartyDetails.length = 0;
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView() });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('getAllPartyDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseFileView().defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayloadCaseFileView().addAnotherDefendant,
    payLoad: submitCaseApiData.submitCasePayloadCaseFileView()
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


test.describe('case management - case worker selects Breathing Space', () => {
  test.beforeEach(async () => {
    await test.step('Given the caseworker selects the "Enter breathing space" event', async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enterBreathingSpaceEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enterBreathingSpace.mainHeader);
    });
  });

  test(
    'caseworker enters breathing space details and validates',
    {
      tag: ['@CM', '@regression'],
      annotation: [
        { type: 'jira', description: 'https://hmcts.atlassian.net/browse/HDPI-6289?' },
        { type: 'ac', description: 'AC01, AC02, AC03, AC04, AC05, AC06, AC07, AC08, AC09' },
      ],
    },
    async () => {
      await test.step('Then the initial case details are displayed correctly and Previous button is hidden', async () => {
        await performAction('validateEnterBreathingSpacePage');
      });

      await test.step('When the caseworker enters a reference number over 14 characters, the validation error is verified', async () => {
      await performAction('validateReferenceNumberLengthError');
      });

      await test.step('When the caseworker enters a future start date, the date error is verified', async () => {
      await performAction('validateFutureStartDateError');
   });


      await test.step('When the caseworker completes the Breathing Space configuration and submits the valid form', async () => {
      await performAction('completeBreathingSpaceFormHappyPath', enterBreathingSpace.happyPathConfig);
      });
        
      await test.step('Then they are directed to Check Your Answers', async () => {
      await performValidation('mainHeader', enterBreathingSpacecheckYourAnswers.mainHeader);
      });
    }
  );

 test(
  'cancelling enter breathing space returns the caseworker to the case dashboard',
  {
    tag: ['@CM', '@regression'],
    annotation: [
      { type: 'jira', description: 'https://hmcts.atlassian.net/browse/HDPI-6289?' },
      { type: 'ac', description: 'AC10: Cancel "Enter Breathing Space" and ensure no data is saved' },
    ],
  },
  async () => {
    await test.step('When the caseworker enters some details and cancels', async () => {
      await performAction('enterBreathingSpaceDetailsAndCancel', enterBreathingSpace.cancelConfig);
    });

    await test.step('Then they are returned to the case dashboard', async () => {
      await performValidation('mainHeader', enterBreathingSpace.cancelReturnHeader);
    });

    await test.step('And reopening the event shows the details were not saved', async () => {
      await performAction('reopenBreathingSpaceEvent');
      await performValidation('elementNotToBeVisible', {
          elementType: 'text',
          text: enterBreathingSpace.cancelConfig.referenceNumber,
        });
      });
  }
);

test(
    'caseworker validates mental health crisis moratorium expected end date visibility',
    {
      tag: ['@CM', '@regression'],
      annotation: [
        { type: 'jira', description: 'https://hmcts.atlassian.net/browse/HDPI-6289?' },
        { type: 'ac', description: 'AC06: When Mental Health Crisis Moratorium is selected, the expected end date question must not be displayed' },
      ],
    },
    async () => {
      await test.step('When the caseworker selects "Mental Health Crisis Moratorium"', async () => {
        await performAction('selectBreathingSpaceType', { type: 'mentalHealthCrisisMoratorium' });
      });

      await test.step('Then the "When is the expected end date?" question must not be displayed', async () => {
       await performValidation('elementNotToBeVisible', enterBreathingSpace.whenIsExpectedEndDateHiddenTextLabel); 
      });
    }
  );
});



