import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { home } from '@data/page-data';
import { createCaseApiWalesData } from '@data/api-data/createCaseWales.api.data';
import { submitCaseApiDataWales } from '@data/api-data/submitCaseWales.api.data';

test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  if (testInfo.title.includes('CaseFile')) {
    await performAction('createCaseAPI', { data: createCaseApiWalesData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiDataWales.submitCasePayloadCaseFileView });
    await performAction('getCaseAPI', 'Claim Submission Time');
    await performAction('fetchCurrentUserAPI', 'Claimant');
  } else {
    await performAction('createCaseAPI', { data: createCaseApiWalesData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiDataWales.submitCasePayloadCaseSummary });
    await performAction('getCaseAPI', 'Claim Submission Time');
    await performAction('fetchCurrentUserAPI', 'Claimant');
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

test.describe('[Case tabs - Wales Journey] @nightly', async () => {
  test('Case tabs Wales - Summary tab test @MAC @regression', async () => {
    await performAction('clickTab', home.caseSummary);
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Address of property',
      table: 'Address of property to be repossessed'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Grounds of possession Wales',
      table: 'Grounds for possession'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Reasons for possession Wales',
      table: 'Reasons for possession',
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Claimant details',
      table: 'Claimant'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Defendant details',
      table: 'Defendant 1'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Rent arrears',
      table: 'Details of rent arrears'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Occupation contract or licence',
      table: 'Occupation contract or licence details'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Notice',
      table: 'Notice details'
    });
  });
  
  test('Case tabs Wales - Case Details tab test @MAC @regression', async () => {
    await performValidation('mainHeader', home.caseSummary)
    await performAction('clickTab', home.caseDetails);
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Claim details',
      table: 'Claim details'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Address of property',
      table: 'Address of property to be repossessed'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Grounds of possession Wales',
      table: 'Grounds for possession'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Occupation contract or licence Case details',
      table: 'Occupation contract or licence details'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Notice Case details Wales',
      table: 'Notice details'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Actions taken',
      table: 'Actions already taken'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Rent arrears Case details Wales',
      table: 'Details of rent arrears'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Reasons for possession Case details Wales',
      table: 'Reasons for possession'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Antisocial behaviour and illegal or prohibited conduct',
      table: 'Antisocial behaviour and illegal or prohibited conduct'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Applications',
      table: 'Applications'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Claimant details',
      table: 'Claimant'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Claimant address',
      table: 'Claimant address for service'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Claimant contact details',
      table: 'Claimant contact details'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Claimant registration and licensing',
      table: 'Claimant registration and licensing'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Claimant circumstances',
      table: 'Claimant circumstances'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Defendant Case details',
      table: 'Defendant 1'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Defendant circumstances',
      table: 'Defendant circumstances'
    });

    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Underlessee or mortgagee',
      table: 'Underlessee or mortgagee 1'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Prohibited conduct standard contract',
      table: 'Prohibited conduct standard contract claim'
    });
    await performAction('validateCaseSummaryDetails', {
      defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseSummary.defendant1.nameKnown,
      additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseSummary.addAnotherDefendant,
      createPayload: createCaseApiWalesData.createCasePayload,
      submitPayload: submitCaseApiDataWales.submitCasePayloadCaseSummary,
      section: 'Required Documents',
      table: 'Required documents'
    });

  });

  test('Case tabs - CaseFile View test @MAC @regression', async () => {
      await performValidation('mainHeader', home.caseSummary)
      await performAction('clickTab', home.caseFileView);
      await performAction('validateCaseFileViewFolders');
      await performAction('validateCaseFileViewIndividualFolder', {
        folder: 'Property documents',
        submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView,
      });
      await performAction('validateCaseFileViewIndividualFolder', {
        folder: 'Statements of case',
        submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView,
      });
      await performAction('validateCaseFileViewIndividualFolder', {
        folder: 'Evidence',
        submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView,
      });
      await performAction('validateCaseFileViewIndividualFolder', {
        folder: 'Correspondence',
        submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView,
      });
      await performAction('validateCaseFileViewIndividualFolder', {
        folder: 'Uncategorised documents',
        submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView,
      });
    });
});