import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import {initializeExecutor, performValidation} from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {caseSummary, home, user} from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
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

test.beforeEach(async ({ page, context }, testInfo) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload() });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
  await performAction('updatePaymentAPI');
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


test('Case management - As a Judicial user successfully Add Judicial Notes to a case',
  {
    tag: ['@CM, @regression'],
    annotation: [
      { type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-8682` },
      { type: 'ac', description: 'AC1 to AC7' },
    ],
  },
  async () => {
  const judicialNoteText = CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText1);
  const currentTime = getCurrentBSTTime();

await test.step(`Given the user selects the "Add a judicial note" event from the event dropdown`, async () => {
  await performAction('selectAnEvent', {eventType: caseSummary.addJudicialNote});
  await performValidation('mainHeader', addJudicialNotes.mainHeader);
});

await test.step(`And the user validates the error messages on the Add Judicial Notes screen`, async () => {
  await performAction('errorValidationAddJudicialNotes', addJudicialNotes.errorValidation);
});

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

test('Case management – As a Judicial user, successfully add multiple judicial notes to a case.',
  {
    tag: ['@CM, @regression'],
    annotation: [
      { type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-8682` },
      { type: 'ac', description: 'AC8' },
    ],
  },
  async () => {
  const judicialNote1Text  = CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText1);
  const currentTime1 = getCurrentBSTTime();
  const judicialNote2Text  = CaseManagementCommonUtils.generateRandomString(addJudicialNotes.notesAboutThisCaseInputText2);
  const currentTime2 = getCurrentBSTTime();

  await test.step(`Given user select an event "Add a Judicial Note" from event drop down`, async () => {
      await performAction('selectAnEvent', {eventType: caseSummary.addJudicialNote});
      await performValidation('mainHeader', addJudicialNotes.mainHeader);
  });

  await test.step(`When the user adds the judicial note1`, async () => {
      await performAction('addJudicialNotes', {
      label: addJudicialNotes.notesAboutThisCaseTextLabel,
      noteTextInput: judicialNote1Text,
      nextPage: checkYourAnswersJudicialNotes.mainHeader
    });
      await performAction('clickButton', checkYourAnswersJudicialNotes.submitButton);
  });

  await test.step(`And the user confirms the  judicial note1 has been added successfully`, async () => {
      await performAction('confirmAddJudicialNotes');
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a judicial note');
  });

  await test.step(`And the user selects the "Add a judicial note" event again to add a second note`, async () => {
      await performAction('selectAnEvent', {eventType: caseSummary.addJudicialNote});
      await performValidation('mainHeader', addJudicialNotes.mainHeader);
    });

  await test.step(`When the user adds the judicial note2`, async () => {
      await performAction('addJudicialNotes', {
        label: addJudicialNotes.notesAboutThisCaseTextLabel,
        noteTextInput: judicialNote2Text,
        nextPage: checkYourAnswersJudicialNotes.mainHeader
      });
      await performAction('clickButton', checkYourAnswersJudicialNotes.submitButton);
    });

  await test.step(`And the user confirms the judicial2 note has been added successfully`, async () => {
      await performAction('confirmAddJudicialNotes');
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a judicial note');
    });

  await test.step(`Then the user verifies that both judicial notes are displayed in reverse chronological order on the Judicial Notes tab`, async () => {
      await performAction('clickTab', home.judicialNotes);
      await performAction('validateJudgeNotesTab', {
        table: 'Note 2',
        userInput: judicialNote2Text,
        createdOn: currentTime2,
        index: 0
      });
      await performAction('validateJudgeNotesTab', {
        table: 'Note 1',
        userInput: judicialNote1Text,
        createdOn: currentTime1,
        index: 1
      });
    });
  });


test('Case management - Verify Add Judicial Note event and Note Tab is not available for other users',
  {
    tag: ['@CM, @regression'],
    annotation: [
      { type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-8682` },
      { type: 'ac', description: 'AC9, AC10' },
    ],
  },
async () => {
  await test.step(`Then the user verifies that the "Add a judicial note" event is not available in the event dropdown`, async () => {
    await performValidation('elementNotToBeVisible',
      { locator: `${caseSummary.nextStepEventList} option`,
        text: caseSummary.addJudicialNote, });
  });
  await test.step(`And the user verifies that the Judicial Notes tab is not visible`, async () => {
    await performValidation('elementNotToBeVisible', home.judicialNotes);
  });
});

