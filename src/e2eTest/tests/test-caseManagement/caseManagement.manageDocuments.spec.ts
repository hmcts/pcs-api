import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performValidation } from '@utils/controller';
import test, { expect } from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { selectDocument } from '@data/page-data-figma/page-data-caseManagement-figma';

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');

  for (const defendant of defendantUserDetails) {
    await performAction('makeAnApplicationAPI', {
      data: makeAnApplicationApiData.makeAnApplicationAdjournPayload(
        defendant.id,
        defendant.name
      ),
    });
  };
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.staffAdmin);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Case management - Manage documents e2e Journey @nightly', async () => {
  test('Case management - Manage documents - Amend @CM @regression', async () => {
    await performAction('selectAnEvent', { eventType: caseSummary.manageDocuments.amend });
    await performValidation('mainHeader', selectDocument.mainHeader);
    await performAction('errorValidationSelectDocumentPage', selectDocument.errorValidation);
    await performAction('select', selectDocument.whichFolderQuestion, selectDocument.docFolder);
  });
});
