import {createCaseApiData,submitCaseApiData} from '@data/api-data';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import test, {BrowserContext, expect, Page} from '@playwright/test';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {
  confirmIfTheseDocumentsRelateToAnApplication, documentsUploadConfirm, uploadAdditionalDocumentsInformation, uploadYourDocuments
} from "@data/page-data-figma/page-data-legalRepresentative";
import {makeAnApplicationApiData} from "@data/api-data";
import {initializeCMExecutor} from "@utils/controller-caseManagement";
import {getCaseTypeId} from "@utils/common/caseType.utils";
import {VERY_LONG_TIMEOUT} from "../playwright.config";
import {
  checkYourAnswersUploadAdditionalDocs,
} from "@data/page-data-figma/page-data-legalRepresentative/checkYourAnswersUploadAdditionalDocs.page.data";
import { FieldsStore } from '@utils/actions/custom-actions/recordAnsweredFields.action';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';
import { getFormattedDate } from '@utils/common/string.utils';
import { home } from '@data/page-data';



test.use({ storageState: undefined })
let uploadAdditionalDocumentsInformationCL: ReturnType<typeof uploadAdditionalDocumentsInformation>;
test.beforeEach(async ({ page, context }, testInfo) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  FieldsStore.clear();

  const title = testInfo.title;

  const isGenAppsSubmitted = /gen\s*apps\s+submitted/.test(title);

  // Default is single def unless the test title explicitly says "Multi Def".
  const isMultiDef = title.includes('multi def');

  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadDefault });

  // await performAction('submitCaseAPI', {
  //   data: isMultiDef
  //     ? submitCaseApiData.submitCasePayload
  //     : submitCaseApiData.submitCasePayloadDefault,
  // });

  console.log(`Case created with case number: ${process.env.CASE_NUMBER}`);
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');

  const genAppPayload =
    title.includes('ADJOURN')
      ? makeAnApplicationApiData.makeAnApplicationAdjournPayload
      : title.includes('SET_ASIDE')
        ? makeAnApplicationApiData.makeAnApplicationstartSetAsidePayload
        : title.includes('SOMETHING_ELSE')
          ? makeAnApplicationApiData.makeAnApplicationSomethingElseWithNoticePayload
          : title.includes('ADJOURN_WITHOUT_NOTICE')
            ? makeAnApplicationApiData.makeAnApplicationAdjournWithOutNoticePayload
            : undefined;

  if (genAppPayload) {
    //for (const defendant of defendantUserDetails) {
    await performAction('makeAnApplicationAPI', {
      data: genAppPayload(defendantUserDetails[0].id, defendantUserDetails[0].name),
    });
    // }
  }

  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.defendantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');
  uploadAdditionalDocumentsInformationCL = uploadAdditionalDocumentsInformation(test.info().title);
  await page.waitForLoadState();
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});
/* The skipped tests will be enabled again after the completion of Ticket https://tools.hmcts.net/jira/browse/HDPI-7755 */
test.describe('Legal Representative - Upload Documents- e2e Journey @nightly', async () => {

  test.skip('Upload documents when GenApps submitted - Multi def', async () => {
    let docRelatedToOption = `${confirmIfTheseDocumentsRelateToAnApplication.relatedToAdjournRadioOptionHidden} ${getFormattedDate()}`;
    let fileName = confirmIfTheseDocumentsRelateToAnApplication.uploadDocHiddenOption[0];
    let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[0];
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', uploadAdditionalDocumentsInformationCL.mainHeader);
    await performAction('reTryOnCallBackError', uploadAdditionalDocumentsInformationCL.continueButton, confirmIfTheseDocumentsRelateToAnApplication.mainHeader as string);
    await performValidation('mainHeader', confirmIfTheseDocumentsRelateToAnApplication.mainHeader);
    await performAction('verifyDocumentRelatesToApplication', {
      question: confirmIfTheseDocumentsRelateToAnApplication.doTheseDocumentsQuestion,
      option: confirmIfTheseDocumentsRelateToAnApplication.relatedToSetAsideRadioOptionHidden,
      count: defendantUserDetails.length,
    });
    await performValidation('mainHeader', uploadYourDocuments.mainHeader);
    // await performAction('uploadFiles', {
    //   documents: [
    //     {type: uploadYourDocuments.rentStatementDropDownInput, fileName: 'rentStatement.pdf', description: uploadYourDocuments.rentStatementDropDownInput},
    //     {type: uploadYourDocuments.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: uploadYourDocuments.witnessStatementDropDownInput},
    //   ]
    // });

    await performAction('uploadAdditionalDocsLR', {
      documents: [
        { type: uploadYourDocuments.rentStatementClaimantDropDownInput, fileName: fileName, description: uploadYourDocuments.rentStatementDropDownInput },
      ]
    });
    await performValidation('mainHeader', checkYourAnswersUploadAdditionalDocs.mainHeader);
    await performAction('reTryOnCallBackError', checkYourAnswersUploadAdditionalDocs.submitButton, documentsUploadConfirm.mainHeader as string);
    await performAction('readDocumentsSubmit');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Upload additional documents');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Property documents',
      submitPayload: submitCaseApiData.submitCasePayloadDefault,
      defendantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
    });
  });

  test('Upload documents when GenApps submitted - Single def SET_ASIDE @regression', async () => {
    let docRelatedToOption = `${confirmIfTheseDocumentsRelateToAnApplication.relatedToSetAsideRadioOptionHidden} ${getFormattedDate()}`;
    let fileName = confirmIfTheseDocumentsRelateToAnApplication.uploadDocHiddenOption[1];
    let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[0];
    await performAction('selectAnEvent', { eventType: caseSummary.uploadAdditionalDocuments });
    await performValidation('mainHeader', uploadAdditionalDocumentsInformationCL.mainHeader);
    await performAction('reTryOnCallBackError', uploadAdditionalDocumentsInformationCL.continueButton, confirmIfTheseDocumentsRelateToAnApplication.mainHeader as string);
    await performAction('selectDocumentRelatingTo', {
      question: confirmIfTheseDocumentsRelateToAnApplication.doTheseDocumentsQuestion,
      option: docRelatedToOption,
      nextPage: uploadYourDocuments.mainHeader,
    });
    await performAction('uploadAdditionalDocsLR', {
      documents: [
        { type: uploadYourDocuments.rentStatementDropDownInput, fileName: fileName, description: uploadYourDocuments.rentStatementClaimantDropDownInput },
      ]
    });
    await performValidation('mainHeader', checkYourAnswersUploadAdditionalDocs.mainHeader);
    await performAction('reTryOnCallBackError', checkYourAnswersUploadAdditionalDocs.submitButton, documentsUploadConfirm.mainHeader as string);
    await performAction('readDocumentsSubmit');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Upload additional documents');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Property documents',
      submitPayload: submitCaseApiData.submitCasePayloadDefault,
      defendantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
    });

  });

  test.skip('Upload documents when GenApps submitted With Out Notice - Multi def', async ({ page }) => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', confirmIfTheseDocumentsRelateToAnApplication.mainHeader);
    await performAction('verifyDocumentRelatesToApplication', {
      question: confirmIfTheseDocumentsRelateToAnApplication.doTheseDocumentsQuestion,
      option: confirmIfTheseDocumentsRelateToAnApplication.relatedToAdjournRadioOptionHidden,
      count: defendantUserDetails.length,
    });
    await performValidation('mainHeader', uploadYourDocuments.mainHeader);
    await performAction('uploadFiles', {
      documents: [
        { type: uploadYourDocuments.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: uploadYourDocuments.witnessStatementDropDownInput },
      ]
    });
    await performValidation('mainHeader', checkYourAnswersUploadAdditionalDocs.mainHeader);
    await performAction('retrieveCYATableDataLR', { name: 'check your answers table' });
    await performAction('validateCYAForLR');
    await performValidation('mainHeader', documentsUploadConfirm.mainHeader);
    await performAction('readDocumentsSubmit');
    await performAction('clickLink', documentsUploadConfirm.signOutLink);
    await page.context().clearCookies();
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await performAction('login', user.defendantSolicitor2);
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    await expect(async () => {
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', confirmIfTheseDocumentsRelateToAnApplication.mainHeader);
    await performValidation('elementNotToBeVisible', {
      elementType: 'text',
      text: confirmIfTheseDocumentsRelateToAnApplication.relatedToAdjournRadioOptionHidden,
    });
  });

  test.skip('Upload documents when GenApps not submitted - Multi def', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', uploadYourDocuments.mainHeader);
    await performAction('uploadFiles', {
      documents: [
        { type: uploadYourDocuments.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: uploadYourDocuments.witnessStatementDropDownInput },
        { type: uploadYourDocuments.rentStatementDropDownInput, fileName: 'rentStatement.pdf', description: uploadYourDocuments.rentStatementDropDownInput },
        { type: uploadYourDocuments.tenancyAgreementDropDownInput, fileName: 'tenancy.pdf', description: uploadYourDocuments.tenancyAgreementDropDownInput },
        { type: uploadYourDocuments.correspondenceFromClaimantDropDownInput, fileName: 'correspondenceFromClaimant.pdf', description: uploadYourDocuments.correspondenceFromClaimantDropDownInput },
        { type: uploadYourDocuments.correspondenceFromDefendantDropDownInput, fileName: 'correspondenceFromDefendant.pdf', description: uploadYourDocuments.correspondenceFromDefendantDropDownInput },
        { type: uploadYourDocuments.photographicEvidenceDropDownInput, fileName: 'photographicEvidence.pdf', description: uploadYourDocuments.photographicEvidenceDropDownInput },
        { type: uploadYourDocuments.certificateOfSuitabilityDropDownInput, fileName: 'certificateOfSuitability.pdf', description: uploadYourDocuments.certificateOfSuitabilityDropDownInput },
        { type: uploadYourDocuments.legalAidCertificateDropDownInput, fileName: 'legalAidCertificate.pdf', description: uploadYourDocuments.legalAidCertificateDropDownInput },
        { type: uploadYourDocuments.otherDocumentDropDownInput, fileName: 'otherDocument.pdf', description: uploadYourDocuments.otherDocumentDropDownInput },
      ]
    });
    await performValidation('mainHeader', checkYourAnswersUploadAdditionalDocs.mainHeader);
  });

  test('Upload documents when GenApps not submitted - Single def @regression', async () => {

    let fileName = confirmIfTheseDocumentsRelateToAnApplication.uploadDocHiddenOption[1];
    let fileName1 = confirmIfTheseDocumentsRelateToAnApplication.uploadDocHiddenOption[3];
    let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[0];
    await performAction('selectAnEvent', { eventType: caseSummary.uploadAdditionalDocuments });
    await performValidation('mainHeader', uploadAdditionalDocumentsInformationCL.mainHeader);
    await performAction('reTryOnCallBackError', uploadAdditionalDocumentsInformationCL.continueButton, uploadYourDocuments.mainHeader as string);
    await performAction('uploadAdditionalDocsLR', {
      documents: [
        { type: uploadYourDocuments.rentStatementDropDownInput, fileName: fileName, description: uploadYourDocuments.rentStatementClaimantDropDownInput },
        { type: uploadYourDocuments.witnessStatementDropDownInput, fileName: fileName1, description: uploadYourDocuments.witnessStatementDropDownInput }
      ]
    });
    await performValidation('mainHeader', checkYourAnswersUploadAdditionalDocs.mainHeader);
    await performAction('reTryOnCallBackError', checkYourAnswersUploadAdditionalDocs.submitButton, documentsUploadConfirm.mainHeader as string);
    await performAction('readDocumentsSubmit');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Upload additional documents');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Property documents',
      submitPayload: submitCaseApiData.submitCasePayloadDefault,
      defendantLRUpload: CaseManagementCommonUtils.renameDocument(fileName)
    });
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Evidence',
      submitPayload: submitCaseApiData.submitCasePayloadDefault,
      defendantLRUpload: CaseManagementCommonUtils.renameDocument(fileName1)
    });
  });

  test('Upload documents when GenApps submitted - Single def ADJOURN_WITHOUT_NOTICE', async ({ page, context }) => {
    let docRelatedToOption = `${confirmIfTheseDocumentsRelateToAnApplication.relatedToAdjournRadioOptionHidden} ${getFormattedDate()}`;
    let fileName = confirmIfTheseDocumentsRelateToAnApplication.uploadDocHiddenOption[1];
    let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[0];
    await performAction('selectAnEvent', { eventType: caseSummary.uploadAdditionalDocuments });
    await performValidation('mainHeader', uploadAdditionalDocumentsInformationCL.mainHeader);
    await performAction('reTryOnCallBackError', uploadAdditionalDocumentsInformationCL.continueButton, confirmIfTheseDocumentsRelateToAnApplication.mainHeader as string);
    await performAction('selectDocumentRelatingTo', {
      question: confirmIfTheseDocumentsRelateToAnApplication.doTheseDocumentsQuestion,
      option: docRelatedToOption,
      nextPage: uploadYourDocuments.mainHeader,
    });
    await performAction('uploadAdditionalDocsLR', {
      documents: [
        { type: uploadYourDocuments.rentStatementDropDownInput, fileName: fileName, description: uploadYourDocuments.rentStatementClaimantDropDownInput },
      ]
    });
    await performValidation('mainHeader', checkYourAnswersUploadAdditionalDocs.mainHeader);
    await performAction('reTryOnCallBackError', checkYourAnswersUploadAdditionalDocs.submitButton, documentsUploadConfirm.mainHeader as string);
    await performAction('readDocumentsSubmit');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Upload additional documents');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Property documents',
      submitPayload: submitCaseApiData.submitCasePayloadDefault,
      defendantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
    });
    await clearBrowserSession(page, context);
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}`);
    const pages = page.context().pages();
    const firstTab = pages[0];
    await firstTab.bringToFront();
    await dismissCookieBanner(page, 'additional');
    await performAction('login', { email: user.claimantSolicitor.email, password: process.env.IDAM_PCS_USER_PASSWORD });
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    await performValidation('mainHeader', home.caseSummary);
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Property documents',
      submitPayload: submitCaseApiData.submitCasePayloadDefault,
      claimantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
    });

  });
});

async function clearBrowserSession(page: Page, context: BrowserContext): Promise<void> {
  await context.clearCookies();
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  const storageInfo = await page.evaluate(() => ({
    localStorageLength: localStorage.length,
    sessionStorageLength: sessionStorage.length,
  }));

  const cookies = await context.cookies();
  expect(cookies, 'Checking if all the cookies have cleared').toHaveLength(0);
  expect(storageInfo.localStorageLength, 'Checking if local storage have cleared').toBe(0);
  expect(storageInfo.sessionStorageLength, 'Checking if session storage have cleared').toBe(0);

}
