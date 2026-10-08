import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import {initializeExecutor, performValidation} from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {caseSummary, home, user} from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performActions } from '@utils/controller-caseManagement';
import {
  addJudicialNotes,
  checkYourAnswersJudicialNotes
} from "@data/page-data-figma/page-data-caseManagement-figma";
import {getCurrentBSTTime} from "@utils/common/string.utils";
import {
  CaseManagementCommonUtils
} from "@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action";


test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }, testInfo) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await performActions(
    'Create a case and make the payment',
    ['createCaseAPI', { data: createCaseApiData.createCasePayload}],
    ['submitCaseAPI', { data: submitCaseApiData.submitCasePayload()}],
    ['getAddressInfo', { data: createCaseApiData.createCasePayload}],
    ['updatePaymentAPI']
  );
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  if (testInfo.title.includes('Judicial user')) {
    await performAction('login', user.judicialJudgeUser);
  }
  else
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

test.describe('Case management - Add Judicial Notes @nightly', async () => {
  test('Case management - As a Judicial user successfully Add Judicial Notes from the case @CM @regression @smoke', async () => {
    await performAction('When the user selects an event', {eventType: caseSummary.addJudicialNote});
    await performAction('Then the user performs error validation for Add Judicial Notes', addJudicialNotes.errorValidation);
    await performAction('When the user adds a Judicial Note', {
      label: addJudicialNotes.notesAboutThisCaseTextLabel,
      noteTextInput: CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText1),
      nextPage: checkYourAnswersJudicialNotes.mainHeader
    });

    await performAction('clickButton', checkYourAnswersJudicialNotes.submitButton);
    await performAction('Then the user confirms Add Judicial Notes');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a judicial note');
    await performActions(
      'Verify that the Judicial notes appear on judicial notes tab',
      ['clickTab', home.judicialNotes],
      ['Then the user validates the Judge Notes tab', {
        table: 'Note 1',
        userInput: CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText1),
        createdOn: getCurrentBSTTime()
      }]
    );
  });

  test('Case management - As a Judicial user successfully Add Multiple Judicial Notes from the case @CM @regression', async () => {
    await performAction('When the user selects an event', {eventType: caseSummary.addJudicialNote});
    await performAction('When the user adds a Judicial Note', {
      label: addJudicialNotes.notesAboutThisCaseTextLabel,
      noteTextInput: CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText1),
      nextPage: checkYourAnswersJudicialNotes.mainHeader
    });
    await performAction('clickButton', checkYourAnswersJudicialNotes.submitButton);
    await performAction('Then the user confirms Add Judicial Notes');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a judicial note');
    await performAction('When the user selects an event', {eventType: caseSummary.addJudicialNote});
    await performAction('When the user adds a Judicial Note', {
      label: addJudicialNotes.notesAboutThisCaseTextLabel,
      noteTextInput: CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText2),
      nextPage: checkYourAnswersJudicialNotes.mainHeader
    });
    await performAction('clickButton', checkYourAnswersJudicialNotes.submitButton);
    await performAction('Then the user confirms Add Judicial Notes');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a judicial note');
    await performActions(
      'Verify Judicial Notes appear in reverse chronological order on Judicial Notes tab',
      ['clickTab', home.judicialNotes],
      ['Then the user validates the Judge Notes tab', {
        table: 'Note 2',
        userInput: CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText2),
        createdOn: getCurrentBSTTime(),
        index: 0
      }],
      ['Then the user validates the Judge Notes tab', {
        table: 'Note 1',
        userInput: CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText1),
        createdOn: getCurrentBSTTime(),
        index: 1
      }]
    );
  });

  test('Case management - Verify Add Judicial Note event is not available for other users @CM @regression', async () => {
    await performValidation('elementNotToBeVisible', {
        locator: `${caseSummary.nextStepEventList} option`,
        text: caseSummary.addJudicialNote,
      });
    await performValidation('elementNotToBeVisible', home.judicialNotes);
  });
});
