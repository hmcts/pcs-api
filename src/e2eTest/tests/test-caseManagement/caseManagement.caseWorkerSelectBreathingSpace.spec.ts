import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';
import { enterBreathingSpace, enterBreathingSpacecheckYourAnswers} from '@data/page-data-figma/page-data-caseManagement-figma';


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
    'caseworker enters breathing space details, validates the check your answers screen, amends the reference number, and submits successfully',
    {
      tag: ['@CM', '@regression'],
      annotation: [
        { type: 'jira', description: 'https://hmcts.atlassian.net/browse/HDPI-6289?||https://hmcts.atlassian.net/browse/HDPI-6290?' },
        { type: 'ac', description: '6289 -AC01, AC02, AC03, AC04, AC05, AC06, AC07, AC08, AC09 || 6290 -AC01, AC02, AC03, AC05,' },
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
        
      await test.step('Then they are directed to Check Your Answers with the case information and answers displayed', async () => {
      await performAction('validateCheckYourAnswersPage', enterBreathingSpacecheckYourAnswers.happyPathConfig);
     });

     await test.step('When the caseworker changes the reference number, the update is retained', async () => {
     await performAction('changeReferenceNumberAndValidate', enterBreathingSpacecheckYourAnswers.changeConfig);
     });

     await test.step('When the caseworker submits, the confirmation screen is displayed', async () => {
     await performAction('submitCheckYourAnswers');
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

test(
    'cancelling from check your answers returns to the case dashboard without saving',
    {
      tag: ['@CM', '@regression'],
      annotation: [
        { type: 'jira', description: 'https://hmcts.atlassian.net/browse/HDPI-6290?' }, // Update with your actual Jira ID if needed
        { type: 'ac', description: 'AC06: Cancel from Check Your Answers and ensure no data is saved' },
      ],
    },
    async () => {
      await test.step('Given the caseworker completes the form and is on Check Your Answers screen', async () => {
        await performAction('completeBreathingSpaceFormHappyPath', enterBreathingSpace.happyPathConfig); 
        await performAction('validateCheckYourAnswersPage', enterBreathingSpacecheckYourAnswers.happyPathConfig);
      });

      await test.step('When they select Cancel, they are returned to the case summary dashboard', async () => {
      await performAction('cancelCheckYourAnswersAndValidate', enterBreathingSpacecheckYourAnswers.happyPathConfig);
      });

      await test.step('Then reopening the breathing space event verifies that the form configuration fields were not saved', async () => {
        await performAction('reopenBreathingSpaceEvent');
        await performValidation('validateInputTextValues', 
          { textLabel: enterBreathingSpace.referenceNumberTextLabel }, 
          { expected: '' }
        );
      });
    }
  );
    test(
    'previous on check your answers returns to enter breathing space with answers retained',
    {
      tag: ['@CM', '@regression'],
      annotation: [
        { type: 'jira', description: 'https://hmcts.atlassian.net/browse/HDPI-6290?' },
        { type: 'ac', description: 'AC04: Return to Enter Breathing Space Screen via Previous' },
      ],
    },
    async () => {
      await test.step('Given the caseworker completes the form and is on Check Your Answers screen', async () => {
        await performAction('completeBreathingSpaceFormHappyPath', enterBreathingSpace.happyPathConfig);
        await performAction('validateCheckYourAnswersPage', enterBreathingSpacecheckYourAnswers.happyPathConfig);
      });

      await test.step('When they select Previous, they can directly amend the pre-populated details and return to CYA screen and validate the changes', async () => {
        await performAction('clickPreviousAndValidateAnswersRetained');
      });
    }
  );

});


