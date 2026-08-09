import {createCaseApiData,submitCaseApiData} from '@data/api-data';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import test from '@playwright/test';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {
  confirmIfTheseDocumentsRelateToAnApplication,uploadYourDocuments
} from "@data/page-data-figma/page-data-legalRepresentative";
import {makeAnApplicationApiData} from "@data/api-data";
import {initializeCMExecutor} from "@utils/controller-caseManagement";


test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }, testInfo) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  FieldsStore.clear();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  console.log(`Case created with case number: ${process.env.CASE_NUMBER}`);
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  if (testInfo.title.includes('GenApps submitted')) {
    const defendant = defendantUserDetails[0];
    await performAction('makeAnApplicationAPI', {
      data: makeAnApplicationApiData.makeAnApplicationAdjournPayload(
        defendant.id,
        defendant.name
      ),
    });
    await performAction('makeAnApplicationAPI', {
      data: makeAnApplicationApiData.makeAnApplicationstartSetAsidePayload(
        defendant.id,
        defendant.name
      ),
    });
    await performAction('makeAnApplicationAPI', {
      data: makeAnApplicationApiData.makeAnApplicationstartSomethingElsePayload(
        defendant.id,
        defendant.name
      ),
    });
  }
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.defendantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');
  await page.waitForLoadState();
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});
test.describe('Legal Representative - Upload Documents- e2e Journey @nightly', async () => {
  test('Upload documents when GenApps submitted @smoke @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    //await performAction('clickButton', uploadAdditionalDocumentsInformation.continueButton);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', confirmIfTheseDocumentsRelateToAnApplication.mainHeader);
    await performAction('verifyDocumentRelatesToApplication', {
      question: confirmIfTheseDocumentsRelateToAnApplication.doTheseDocumentsQuestion,
      option: confirmIfTheseDocumentsRelateToAnApplication.relatedToAdjournRadioOptionHidden,
      count: defendantUserDetails.length,
    });
    await performValidation('mainHeader', uploadYourDocuments.mainHeader);
  });

  test('Upload documents when GenApps not submitted @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', uploadYourDocuments.mainHeader);
  });
});
