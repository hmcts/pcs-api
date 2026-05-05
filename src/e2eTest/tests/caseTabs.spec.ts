import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor, performAction } from '@utils/controller';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { LONG_TIMEOUT } from 'playwright.config';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { home } from '@data/page-data';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseTab });
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Case%20Parties`);
  
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('[Case tabs - England Journey] @nightly', async () => {
  test('Case tabs - Case parties tab test', async () => {
    await expect(async () => {
      await performAction('clickTab', home.caseParties);
    }).toPass({
      timeout: LONG_TIMEOUT,
    });

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

});
