import {createCaseApiData,submitCaseApiData} from '@data/api-data';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import test, { expect } from '@playwright/test';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { home } from '@data/page-data';
import {
  confirmIfTheseDocumentsRelateToAnApplication,
  uploadAdditionalDocumentsInformation, uploadYourDocuments
} from "@data/page-data-figma/page-data-legalRepresentative";
import {initializeGenAppsExecutor} from "@utils/controller-genApps";
import {makeAnApplicationApiData} from "@data/api-data";
import {initializeCMExecutor} from "@utils/controller-caseManagement";

export let  defendantUserDetails: { id: string; name: string }[] = [];

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }, testInfo) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  FieldsStore.clear();
  if (testInfo.title.includes('GenApps not submitted')) {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
    console.log(`Case created with case number: ${process.env.CASE_NUMBER}`);
    await performAction('updatePaymentAPI');
    await performAction('getCaseAPI', 'Link Solicitor');
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await dismissCookieBanner(page, 'additional');
    await performAction('login', user.defendantSolicitor);
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToSummaryPage');
    await page.waitForLoadState();
  } else {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
    console.log(`Case created with case number: ${process.env.CASE_NUMBER}`);
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
    await performAction('updatePaymentAPI');
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await dismissCookieBanner(page, 'additional');
    await performAction('login', user.defendantSolicitor);
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToSummaryPage');
    await page.waitForLoadState();
  }
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});
test.describe('Legal Representative - Upload Documents- e2e Journey @nightly', async () => {
  test('Upload documents when GenApps submitted @smoke @regression @WIP', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', confirmIfTheseDocumentsRelateToAnApplication.mainHeader);
  });

  test('Upload documents when GenApps not submitted @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', uploadYourDocuments.mainHeader);
  });

});
