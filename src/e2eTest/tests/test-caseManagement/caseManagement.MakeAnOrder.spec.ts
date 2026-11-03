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
import {
  CaseManagementCommonUtils
} from "@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action";


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
            {type: 'ac', description: 'AC1 and AC2'},
          ],
        },
        async () => {
        const recitalTextInput = CaseManagementCommonUtils.generateRandomString(makeAnOrder.recitalParagraphTextInput);
        const submitPayload = submitCaseApiData.submitCasePayloadCaseDetails();
          await test.step(`Given the user selects the "Make an Order" event from the event dropdown`, async () => {
            await performAction('selectAnEvent', {eventType: caseSummary.makeAnOrder});
            await  performValidation('mainHeader', makeAnOrder.mainHeader);
          });

          await test.step(`When the user provides input details and click Save and Draft button`, async () => {
            await performAction('makeAnOrderDetailsProvided', {
              submitPayload: submitCaseApiData.submitCasePayloadCaseDetails(),
              claimantRadioOption: makeAnOrder.claimantRadioOption[0],
              defendantRadioOption: makeAnOrder.defendantRadioOption[1],
              label: makeAnOrder.recitalParagraphTextLabel,
              recitalInput: recitalTextInput,
              question: makeAnOrder.possessionLabel,
              option: makeAnOrder.forthwithRadioOption,
              question1:makeAnOrder.groundsLabel,
              option1:makeAnOrder.mandatoryRadioOption,
              question2: makeAnOrder.additionOrderLabel,
              option2: makeAnOrder.transferToHighCourtCheckBox,
              question3: makeAnOrder.costHeader,
              option3: makeAnOrder.costRadioOption[4],
              button: makeAnOrder.saveAsDraftButton
            });
          });
          await test.step(`And the user selects the "Make an Order" event from the event dropdown`, async () => {
            await performAction('selectAnEvent', {eventType: caseSummary.makeAnOrder});
            await performValidation('mainHeader', makeAnOrder.mainHeader);
          });

          await test.step(`And the user verify saved draft order reloads automaticallyS`, async () => {
            await performAction('verifySavedDraftOrderDetails',{
              inputData: [
                {
                  type: 'radio',
                  inputRadioQuestion: `${makeAnOrder.claimantLabel} ${submitPayload.claimantName}`,
                  expectedAnswer: makeAnOrder.claimantRadioOption[0],
                },
                {
                  type: 'radio',
                  inputRadioQuestion: `${makeAnOrder.defendantLabel} ${submitPayload.defendant1.firstName} ${submitPayload.defendant1.lastName}`,
                  expectedAnswer: makeAnOrder.defendantRadioOption[1],
                },
                {
                  type: 'checkBox',
                  locatorType: 'label',
                  inputCheckBoxQuestion: makeAnOrder.recitalsHeader,
                  expectedAnswer: makeAnOrder.recitalCheckBox,

                },
                {
                  type: 'inputText',
                  locatorType: 'label',
                  inputTextLabel: makeAnOrder.recitalParagraphTextLabel,
                  expectedAnswer: recitalTextInput,
                },
                {
                  type: 'radio',
                  inputRadioQuestion: makeAnOrder.possessionLabel,
                  expectedAnswer: makeAnOrder.forthwithRadioOption
                },
                {
                  type: 'checkBox',
                  locatorType: 'label',
                  inputCheckBoxQuestion: makeAnOrder.costHeader,
                  expectedAnswer: makeAnOrder.costCheckBox
                },
              ]
            });
          });
        }
      );
    });
