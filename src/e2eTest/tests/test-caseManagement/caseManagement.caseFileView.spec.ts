import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import test, { expect } from '@playwright/test';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { home } from '@data/page-data';


test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('getCaseAPI', 'Link Solicitor');
  
  for (const defendant of defendantUserDetails) {
    await performAction('makeAnApplicationAPI', {
      data: makeAnApplicationApiData.makeAnApplicationAdjournPayload(
        defendant.id,
        defendant.name
      ),
    });
  };
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`, { waitUntil: 'domcontentloaded' });
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

test.describe('Case management - e2e Journey @nightly', async () => {

  test('Case management - CaseFile View test @PR @regression', async () => {
    await performValidation('mainHeader', home.caseSummary)
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Applications',
      submitPayload: makeAnApplicationApiData.makeAnApplicationAdjournPayload(defendantUserDetails[0].id,defendantUserDetails[0].name),
    });
  });
});
