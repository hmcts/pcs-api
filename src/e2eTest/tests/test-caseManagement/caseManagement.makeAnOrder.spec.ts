import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import {initializeExecutor, performValidation} from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {caseSummary, user} from '@data/page-data';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import {
  makeAnOrder,
} from "@data/page-data-figma/page-data-caseManagement-figma";


test.use({ storageState: undefined })
test.beforeEach(async ({page, context}) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await test.step(`Given the user creates a case and completes the payment via API,`, async () => {
    await performAction('createCaseAPI', {data: createCaseApiData.createCasePayload});
    await performAction('submitCaseAPI', {data: submitCaseApiData.submitCasePayloadDefault()});
    await performAction('getAddressInfo', {data: createCaseApiData.createCasePayload});
    await performAction('updatePaymentAPI');
  });
  await test.step('Given the user navigates to the Manage Cases application', async () => {
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  });
  await test.step('When the user logs in with the appropriate role', async () => {
    await performAction('login', user.judicialJudgeUser);
  });
  await test.step('Then the user navigates to the case summary page', async () => {
    await performAction('navigateToSummaryPage');
  });
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('Case management - As a Judge resumes draft order @nightly', async () => {
  test('Case management - Verify the Judge user can save an order as a draft',
    {
      tag: ['@CM, @regression'],
      annotation: [
        {type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-7032`},
        {type: 'ac', description: 'AC1 to AC2'},
      ],
    },
    async () => {
      await test.step(`Given the user selects the "Make an Order" event from the event dropdown`, async () => {
        await performAction('selectAnEvent', {eventType: caseSummary.makeAnOrder});
        await  performValidation('mainHeader', makeAnOrder.mainHeader);
      });

      await test.step(`When the user provides input in Recitals section and click Save and Draft button`, async () => {
        await performAction('inputProvidedInRecitals', {
          submitPayload: submitCaseApiData.submitCasePayloadDefault(),
          label: makeAnOrder.recitalParagraphTextLabel,
          recitalInput: makeAnOrder.recitalParagraphTextInput,
          button: makeAnOrder.saveAsDraftButton
        });
      });
      await test.step(`And the user selects the "Make an Order" event from the event dropdown`, async () => {
        await performAction('selectAnEvent', {eventType: caseSummary.makeAnOrder});
        await performValidation('mainHeader', makeAnOrder.mainHeader);
      });

      await test.step(`And the user verify saved draft order reloads automaticallyS`, async () => {
        await performAction('verifySavedDraftOrderDetails');
      });
    });

  test('Case management - Verify the Judge user creates multiple numbered lists sequentially',
    {
      tag: ['@CM, @regression'],
      annotation: [
        {type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-7447`},
        {type: 'ac', description: 'AC1 and AC2'},
      ],
    },
    async () => {
      await test.step(`Given the user selects the "Make an Order" event from the event dropdown`, async () => {
        await performAction('selectAnEvent', {eventType: caseSummary.makeAnOrder});
        await  performValidation('mainHeader', makeAnOrder.mainHeader);
      });

      await test.step(`When the user provides input in make an order `, async () => {
        await performAction('makeAnOrderDetailsProvided', {
          submitPayload: submitCaseApiData.submitCasePayloadCaseDetails(),
          claimantRadioOption: makeAnOrder.claimantRadioOption[0],
          defendantRadioOption: makeAnOrder.defendantRadioOption[1],
          label: makeAnOrder.recitalParagraphTextLabel,
          recitalInput: makeAnOrder.recitalParagraphTextInput,
          question: makeAnOrder.possessionLabel,
          option: makeAnOrder.forthwithRadioOption,
          question1:makeAnOrder.groundsLabel,
          option1:makeAnOrder.mandatoryRadioOption,
          question2: makeAnOrder.additionOrderLabel,
          option2: makeAnOrder.transferToHighCourtCheckBox,
          question3: makeAnOrder.costHeader,
          option3: makeAnOrder.costRadioOption[4],
        });
        await test.step(`And the user verify order preview section displays the expected numbered list in sequence`, async () => {
         await performAction('verifyPreviewSectionNumberedList');
        });

      });
    });
});
