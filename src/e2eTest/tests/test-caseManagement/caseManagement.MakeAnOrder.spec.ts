import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import {initializeExecutor, performValidation} from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {caseSummary, home, user} from '@data/page-data';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import {
  addJudicialNotes,
  checkYourAnswersJudicialNotes,
} from "@data/page-data-figma/page-data-caseManagement-figma";
import {getCurrentBSTTime} from "@utils/common/string.utils";
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
      await performAction('submitCaseAPI', {data: submitCaseApiData.submitCasePayload()});
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
      test('Case management - As a Judge user resumes draft order',
        {
          tag: ['@CM, @regression'],
          annotation: [
            {type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-7447`},
            {type: 'ac', description: 'AC1 to AC7'},
          ],
        },
        async () => {
          const judicialNoteText = CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText1);
          const currentTime = getCurrentBSTTime();

          await test.step(`Given the user selects the "Add a judicial note" event from the event dropdown`, async () => {
            await performAction('selectAnEvent', {eventType: caseSummary.makeAnOrder});
            await performValidation('mainHeader', addJudicialNotes.mainHeader);
          });

          await test.step(`And the user validates the error messages on the Add Judicial Notes screen`, async () => {
            await performAction('clickButton', {eventType: caseSummary.makeAnOrder});

            await test.step(`When the user adds a judicial note in the text field`, async () => {
              await performAction('addJudicialNotes', {
                label: addJudicialNotes.notesAboutThisCaseTextLabel,
                noteTextInput: judicialNoteText,
                nextPage: checkYourAnswersJudicialNotes.mainHeader
              });
              await performAction('clickButton', checkYourAnswersJudicialNotes.submitButton);
            });

            await test.step(`And the user confirms that the judicial note has been added successfully`, async () => {
              await performAction('confirmAddJudicialNotes');
              await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a judicial note');
            });

            await test.step(`Then the user verifies that the judicial note is displayed on the Judicial Notes tab`, async () => {
              await performAction('clickTab', home.judicialNotes);
              await performAction('validateJudgeNotesTab', {
                table: 'Note 1',
                userInput: judicialNoteText,
                createdOn: currentTime
              });
            });
          });

        });
    });
