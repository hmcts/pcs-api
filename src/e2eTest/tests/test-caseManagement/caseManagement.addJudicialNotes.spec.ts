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
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
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
    const currentTime = getCurrentBSTTime();
    const judicialNoteText = CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText1);
    await performActions(
      `Select event "Add a Judicial Note" from event drop down`,
      ['When the user selects an event', {eventType: caseSummary.addJudicialNote}]
    );
    await performActions(
      'Error validation for Add Judicial Notes screen',
      ['When the user performs error validation for Add Judicial Notes', addJudicialNotes.errorValidation]
    );
    await performActions(
      'Adding Judicial Note',
      ['When the user adds a Judicial Note', {
        label: addJudicialNotes.notesAboutThisCaseTextLabel,
        noteTextInput: judicialNoteText,
        nextPage: checkYourAnswersJudicialNotes.mainHeader
      }]
    );
    await performAction('clickButton', checkYourAnswersJudicialNotes.submitButton);
    await performActions(
      'Validating Add Judicial Notes confirmation screen',
      ['When the user confirms Add Judicial Notes']
    );
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a judicial note');
    await performActions(
      'Verify that the Judicial notes appear on judicial notes tab',
      ['clickTab', home.judicialNotes],
      ['Then the user validates the Judge Notes tab', {
        table: 'Note',
        userInput: judicialNoteText,
        createdOn: currentTime
      }]
    );
  });

  test('Case management - As a Judicial user successfully Add Multiple Judicial Notes from the case @CM @regression', async () => {
    const judicialNote1Text  = CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText1);
    const currentTime1 = getCurrentBSTTime();
    const judicialNote2Text  = CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText2);
    const currentTime2 = getCurrentBSTTime();
    await performActions(
      `Select event "Add a Judicial Note" from event drop down`,
      ['When the user selects an event', {eventType: caseSummary.addJudicialNote}]
    );
    await performActions(
      'Adding Judicial Note1',
      ['When the user adds a Judicial Note', {
        label: addJudicialNotes.notesAboutThisCaseTextLabel,
        noteTextInput: judicialNote1Text,
        nextPage: checkYourAnswersJudicialNotes.mainHeader
      }]
    );
    await performAction('clickButton', checkYourAnswersJudicialNotes.submitButton);
    await performActions(
      'Validating Add Judicial Notes1 confirmation screen',
      ['When the user confirms Add Judicial Notes']
    );
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a judicial note');
    await performActions(
      `Select event "Add a Judicial Note" from event drop down`,
      ['When the user selects an event', {eventType: caseSummary.addJudicialNote}]
    );
    await performActions(
      'Adding Judicial Note2',
      ['When the user adds a Judicial Note', {
        label: addJudicialNotes.notesAboutThisCaseTextLabel,
        noteTextInput: judicialNote2Text,
        nextPage: checkYourAnswersJudicialNotes.mainHeader
      }]
    );
    await performAction('clickButton', checkYourAnswersJudicialNotes.submitButton);
    await performActions(
      'Validating Add Judicial Notes2 confirmation screen',
      ['When the user confirms Add Judicial Notes']
    );
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a judicial note');
    await performActions(
      'Verify Judicial Notes appear in reverse chronological order on Judicial Notes tab',
      ['clickTab', home.judicialNotes],
      ['Then the user validates the Judge Notes tab', {
        table: 'Note 2',
        userInput: judicialNote2Text,
        createdOn: currentTime2,
        index: 0
      }],
      ['Then the user validates the Judge Notes tab', {
        table: 'Note 1',
        userInput: judicialNote1Text,
        createdOn: currentTime1,
        index: 1
      }]
    );
  });

  test('Case management - Verify Add Judicial Note event is not available for other users @CM @regression', async () => {
    await performActions(
      `Verify "Add a judicial note" event is not available`,
      ['elementNotToBeVisible', {
        locator: `${caseSummary.nextStepEventList} option`,
        text: caseSummary.addJudicialNote
      }]
    );
  });
});
