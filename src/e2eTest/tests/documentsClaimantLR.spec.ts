import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import test, { BrowserContext, expect, Page } from '@playwright/test';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {
  confirmIfTheseDocumentsRelateToAnApplication, documentsUploadConfirm, uploadAdditionalDocumentsInformation, uploadYourDocuments
} from "@data/page-data-figma/page-data-legalRepresentative";
import { makeAnApplicationApiData } from "@data/api-data";
import { initializeCMExecutor } from "@utils/controller-caseManagement";
import { getCaseTypeId } from "@utils/common/caseType.utils";
import { VERY_LONG_TIMEOUT } from "../playwright.config";
import {
  checkYourAnswersUploadAdditionalDocs,
} from "@data/page-data-figma/page-data-legalRepresentative/checkYourAnswersUploadAdditionalDocs.page.data";
import { FieldsStore } from '@utils/actions/custom-actions/recordAnsweredFields.action';
import { getFormattedDate } from '@utils/common/string.utils';
import { home } from '@data/page-data';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';


let uploadAdditionalDocumentsInformationCL: ReturnType<typeof uploadAdditionalDocumentsInformation>;
test.beforeEach(async ({ page, context }, testInfo) => {
  initializeExecutor(page);
  initializeCMExecutor(page);
  FieldsStore.clear();

  const title = testInfo.title;
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  //await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
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
  await performAction('navigateToSummaryPage');
  uploadAdditionalDocumentsInformationCL = uploadAdditionalDocumentsInformation(test.info().title);
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Claimant Legal Representative - Upload Documents- e2e Journey @nightly', async () => {

  test('Claimant LR Upload documents when GenApps submitted - Multi def - ADJOURN @smoke @regression', async () => {
    let docRelatedToOption = `${confirmIfTheseDocumentsRelateToAnApplication.relatedToAdjournRadioOptionHidden} ${getFormattedDate()}`;
    let fileName = confirmIfTheseDocumentsRelateToAnApplication.uploadDocHiddenOption[0];
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
      submitPayload: submitCaseApiData.submitCasePayload,
      claimantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
    });

  });

  test('Claimant LR Upload documents when GenApps submitted - SET_ASIDE', async () => {
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
        { type: uploadYourDocuments.rentStatementClaimantDropDownInput, fileName: fileName, description: uploadYourDocuments.rentStatementClaimantDropDownInput },
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
      submitPayload: submitCaseApiData.submitCasePayload,
      claimantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
    });

  });

  test('Claimant LR Upload documents when GenApps submitted - SOMETHING_ELSE', async () => {
    let docRelatedToOption = `${confirmIfTheseDocumentsRelateToAnApplication.relatedToApplicationRadioOptionHidden} ${getFormattedDate()}`;
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
      submitPayload: submitCaseApiData.submitCasePayload,
      claimantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
    });

  });

  test('Claimant LR Upload documents for Claim or CounterClaim submitted - SOMETHING_ELSE', async () => {
    let docRelatedToOption = confirmIfTheseDocumentsRelateToAnApplication.noRadioOption;
    let fileName = confirmIfTheseDocumentsRelateToAnApplication.uploadDocHiddenOption[2];
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
        { type: uploadYourDocuments.witnessStatementDropDownInput, fileName: fileName, description: uploadYourDocuments.rentStatementDropDownInput },
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
      submitPayload: submitCaseApiData.submitCasePayload,
      claimantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
    });

  });

  test('Claimant LR Upload documents when GenApps not submitted - Single def', async () => {

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
      submitPayload: submitCaseApiData.submitCasePayload,
      claimantLRUpload: CaseManagementCommonUtils.renameDocument(fileName)
    });
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Evidence',
      submitPayload: submitCaseApiData.submitCasePayload,
      claimantLRUpload: CaseManagementCommonUtils.renameDocument(fileName1)
    });
  });

  test('Claimant LR Upload documents when GenApps submitted - Single def ADJOURN_WITHOUT_NOTICE', async ({ page, context }) => {
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
          { type: uploadYourDocuments.rentStatementClaimantDropDownInput, fileName: fileName, description: uploadYourDocuments.rentStatementClaimantDropDownInput },
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
        submitPayload: submitCaseApiData.submitCasePayload,
        defendantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
      });
      await clearBrowserSession(page, context);
      await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}`);
      const pages = page.context().pages();
      const firstTab = pages[0];
      await firstTab.bringToFront();
      await dismissCookieBanner(page, 'additional');
      await performAction('login', { email: user.defendantSolicitor.email, password: process.env.IDAM_PCS_USER_PASSWORD });
      await dismissCookieBanner(page, 'analytics');
      await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
      await performValidation('mainHeader', home.caseSummary);
      await performAction('clickTab', home.caseFileView);
      await performAction('validateCaseFileViewFolders', home.caseFileFolders);
      await performAction('validateCaseFileViewIndividualFolder', {
        folder: 'Property documents',
        submitPayload: submitCaseApiData.submitCasePayload,
        allowEmptyFolder: true
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

