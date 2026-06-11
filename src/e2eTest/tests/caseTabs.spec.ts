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

test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  if (testInfo.title.includes('Summary')) {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseSummary });
    await performAction('fetchCurrentUserAPI');
  } else if (testInfo.title.includes('Details')) {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseDetails });
    await performAction('fetchCurrentUserAPI');
  } else {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseTab });
    await performAction('fetchCurrentUserAPI');
  }
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

  test('Case tabs - Summary tab test @MAC @regression', async () => {
    await performAction('clickTab', home.caseSummary);
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseSummary,
      section: 'Address of property',
      table: 'Address of property to be repossessed'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseSummary,
      section: 'Claimant details',
      table: 'Claimant'
    });

    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseSummary,
      section: 'Defendant details',
      table: 'Defendant 1'
    });

    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseSummary,
      section: 'Grounds of possession',
      table: 'Grounds for possession'
    });

    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseSummary,
      section: 'Rent arrears',
      table: 'Details of rent arrears'
    });

    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseSummary,
      section: 'Tenancy and Occupation',
      table: 'Tenancy, occupation contract or licence details'
    });

    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseSummary,
      section: 'Notice',
      table: 'Notice details'
    });

  });

  test('Case tabs - Case Details tab test @MAC @regression', async () => {
    await performAction('clickTab', home.caseDetails);
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Claim details',
      table: 'Claim details'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Address of property',
      table: 'Address of property to be repossessed'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Claimant details',
      table: 'Claimant'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Claimant address',
      table: 'Claimant address for service'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Claimant contact details',
      table: 'Claimant contact details'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Claimant circumstances',
      table: 'Claimant circumstances'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Defendant Case details',
      table: 'Defendant 1'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Defendant circumstances',
      table: 'Defendant circumstances'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Grounds of possession',
      table: 'Grounds for possession'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Rent arrears Case details',
      table: 'Details of rent arrears'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Tenancy and Occupation Case details',
      table: 'Tenancy, occupation contract or licence details'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Notice Case details',
      table: 'Notice details'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Actions taken',
      table: 'Actions already taken'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Reasons for possession',
      table: 'Reasons for possession'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Applications',
      table: 'Applications'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Demotion of tenancy',
      table: 'Demotion of tenancy claim'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Suspension of right to buy',
      table: 'Suspension of right to buy claim'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseDetails.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseDetails.addAnotherDefendant,
      createPayload: createCaseApiData.createCasePayload,
      submitPayload: submitCaseApiData.submitCasePayloadCaseDetails,
      section: 'Underlessee or mortgagee',
      table: 'Underlessee or mortgagee 1'
    });

  });
});
