import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home } from '@data/page-data';
import { addCaseNote } from '@data/page-data-figma';
import { checkYourAnswersCaseNote } from '@data/page-data/checkYourAnswersCaseNote.page.data';
import { getCurrentBSTTime } from '@utils/common/string.utils';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseTab });
  await performAction('fetchCurrentUserAPI');
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('[Case tabs - England Journey] @nightly', async () => {
  test('Case tabs - Case parties tab test @MAC @regression', async () => {
    await performAction('clickTab', home.caseParties);
    await performAction('validateDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseTab.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseTab.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayloadCaseTab,
      table: 'Defendant'
    });

    await performAction('validateClaimantDetails', {
      payLoad: submitCaseApiData.submitCasePayloadCaseTab,
      table: 'Claimant'
    });
  });

  test('Case tabs - Notes tab test @MAC @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.addCaseNote);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', addCaseNote.mainHeader);
    await performAction('addCaseNotes', {
      label: addCaseNote.addNoteTextLabel,
      input: addCaseNote.addNoteTextInput
    })
    const currentTime = getCurrentBSTTime();
    await performValidation('text', {
      "text": checkYourAnswersCaseNote.header,
      "elementType": "subHeading"
    });
  
    await performAction('clickButton', checkYourAnswersCaseNote.submitNote);
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Add a case note');
    await performAction('clickTab', home.caseNotes);
    await performAction('validateCaseNotesDetails', {
      createdOn: currentTime.replace(/:\d{2} /, " "),
      userInput: addCaseNote.addNoteTextInput,
      table: 'Note'
    });
  });

});
