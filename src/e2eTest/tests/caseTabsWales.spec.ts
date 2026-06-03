import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home } from '@data/page-data';
import { addCaseNote } from '@data/page-data-figma';
import { checkYourAnswersCaseNote } from '@data/page-data/checkYourAnswersCaseNote.page.data';
import { getCurrentBSTTime } from '@utils/common/string.utils';
import { createCaseApiWalesData } from '@data/api-data/createCaseWales.api.data';
import { submitCaseApiDataWales } from '@data/api-data/submitCaseWales.api.data';

test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiWalesData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiDataWales.submitCasePayloadCaseSummary });
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
      section: 'Grounds of possession',
      table: 'Grounds for possession'
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
      section: 'Tenancy and Occupation',
      table: 'Tenancy, occupation contract or licence details'
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
});