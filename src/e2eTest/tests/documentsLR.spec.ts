import {createCaseApiData,submitCaseApiData} from '@data/api-data';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import test, {expect} from '@playwright/test';
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

  const title = testInfo.title.toLowerCase();

  const isGenAppsSubmitted = /gen\s*apps\s+submitted/.test(title);

  // Default is single def unless the test title explicitly says "Multi Def".
  const isMultiDef = title.includes('multi def');

  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });

  await performAction('submitCaseAPI', {
    data: isMultiDef
      ? submitCaseApiData.submitCasePayload
      : submitCaseApiData.submitCasePayloadDefault,
  });

  console.log(`Case created with case number: ${process.env.CASE_NUMBER}`);
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPIForLR', 'Link Solicitor');

  if (isGenAppsSubmitted) {
    const defendant = defendantUserDetails[0];
    await performAction('makeAnApplicationAPIForLR', {
      data: makeAnApplicationApiData.makeAnApplicationAdjournWithOutNoticePayload(
        defendant.id,
        defendant.name
      ),
    });
    await performAction('makeAnApplicationAPIForLR', {
      data: makeAnApplicationApiData.makeAnApplicationstartSetAsidePayload(
        defendant.id,
        defendant.name
      ),
    });
    await performAction('makeAnApplicationAPIForLR', {
      data: makeAnApplicationApiData.makeAnApplicationSomethingElseWithNoticePayload(
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
  uploadAdditionalDocumentsInformationCL = uploadAdditionalDocumentsInformation( test.info().title );
  await page.waitForLoadState();
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Legal Representative - Upload Documents- e2e Journey @nightly', async () => {

  test('Upload documents when GenApps submitted - Multi def @smoke @regression', async () => {
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
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView,
      claimantLRUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType)
    });
  });

  test('Upload documents when GenApps submitted - Single def @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', confirmIfTheseDocumentsRelateToAnApplication.mainHeader);
    await performAction('verifyDocumentRelatesToApplication', {
      question: confirmIfTheseDocumentsRelateToAnApplication.doTheseDocumentsQuestion,
      option: confirmIfTheseDocumentsRelateToAnApplication.relatedToSetAsideRadioOptionHidden,
      count: defendantUserDetails.length,
    });
    await performValidation('mainHeader', uploadYourDocuments.mainHeader);
    await performAction('uploadFiles', {
      documents: [
        {type: uploadYourDocuments.rentStatementDropDownInput, fileName: 'rentStatement.pdf', description: uploadYourDocuments.rentStatementDropDownInput},
        {type: uploadYourDocuments.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: uploadYourDocuments.witnessStatementDropDownInput},
      ]
    });
    await performValidation('mainHeader', checkYourAnswersUploadAdditionalDocs.mainHeader);
    await performAction('retrieveCYATableDataLR', { name: 'check your answers table' });
    await performAction('validateCYAForLR');
    await performValidation('mainHeader', documentsUploadConfirm.mainHeader);
    await performAction('readDocumentsSubmit');
  });

  test('Upload documents when GenApps submitted With Out Notice - Multi def @regression', async ({page}) => {
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
        {type: uploadYourDocuments.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: uploadYourDocuments.witnessStatementDropDownInput},
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

  test('Upload documents when GenApps not submitted - Multi def @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', uploadYourDocuments.mainHeader);
    await performAction('uploadFiles', {
      documents: [
        {type: uploadYourDocuments.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: uploadYourDocuments.witnessStatementDropDownInput},
        {type: uploadYourDocuments.rentStatementDropDownInput, fileName: 'rentStatement.pdf', description: uploadYourDocuments.rentStatementDropDownInput},
        {type: uploadYourDocuments.tenancyAgreementDropDownInput, fileName: 'tenancy.pdf', description: uploadYourDocuments.tenancyAgreementDropDownInput},
        {type: uploadYourDocuments.correspondenceFromClaimantDropDownInput, fileName: 'correspondenceFromClaimant.pdf', description: uploadYourDocuments.correspondenceFromClaimantDropDownInput},
        {type: uploadYourDocuments.correspondenceFromDefendantDropDownInput, fileName: 'correspondenceFromDefendant.pdf', description: uploadYourDocuments.correspondenceFromDefendantDropDownInput},
        {type: uploadYourDocuments.photographicEvidenceDropDownInput, fileName: 'photographicEvidence.pdf', description: uploadYourDocuments.photographicEvidenceDropDownInput},
        {type: uploadYourDocuments.certificateOfSuitabilityDropDownInput, fileName: 'certificateOfSuitability.pdf', description: uploadYourDocuments.certificateOfSuitabilityDropDownInput},
        {type: uploadYourDocuments.legalAidCertificateDropDownInput, fileName: 'legalAidCertificate.pdf', description: uploadYourDocuments.legalAidCertificateDropDownInput},
        {type: uploadYourDocuments.otherDocumentDropDownInput, fileName: 'otherDocument.pdf', description: uploadYourDocuments.otherDocumentDropDownInput},
      ]
    });
    await performValidation('mainHeader', checkYourAnswersUploadAdditionalDocs.mainHeader);
  });

  test('Upload documents when GenApps not submitted - Single def @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.uploadAdditionalDocuments);
    await performAction('clickButton', caseSummary.go);
    await performAction('uploadAdditionalDocumentsInfo');
    await performValidation('mainHeader', uploadYourDocuments.mainHeader);
    await performAction('uploadFiles', {
      documents: [
        {type: uploadYourDocuments.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: uploadYourDocuments.witnessStatementDropDownInput},
        {type: uploadYourDocuments.rentStatementDropDownInput, fileName: 'rentStatement.pdf', description: uploadYourDocuments.rentStatementDropDownInput},
        {type: uploadYourDocuments.tenancyAgreementDropDownInput, fileName: 'tenancy.pdf', description: uploadYourDocuments.tenancyAgreementDropDownInput},
        {type: uploadYourDocuments.correspondenceFromClaimantDropDownInput, fileName: 'correspondenceFromClaimant.pdf', description: uploadYourDocuments.correspondenceFromClaimantDropDownInput},
        {type: uploadYourDocuments.correspondenceFromDefendantDropDownInput, fileName: 'correspondenceFromDefendant.pdf', description: uploadYourDocuments.correspondenceFromDefendantDropDownInput},
        {type: uploadYourDocuments.photographicEvidenceDropDownInput, fileName: 'photographicEvidence.pdf', description: uploadYourDocuments.photographicEvidenceDropDownInput},
        {type: uploadYourDocuments.certificateOfSuitabilityDropDownInput, fileName: 'certificateOfSuitability.pdf', description: uploadYourDocuments.certificateOfSuitabilityDropDownInput},
        {type: uploadYourDocuments.legalAidCertificateDropDownInput, fileName: 'legalAidCertificate.pdf', description: uploadYourDocuments.legalAidCertificateDropDownInput},
        {type: uploadYourDocuments.otherDocumentDropDownInput, fileName: 'otherDocument.pdf', description: uploadYourDocuments.otherDocumentDropDownInput},
      ]
    });
    await performValidation('mainHeader', checkYourAnswersUploadAdditionalDocs.mainHeader);
    await performAction('retrieveCYATableDataLR', { name: 'check your answers table' });
    await performAction('validateCYAForLR');
    await performValidation('mainHeader', documentsUploadConfirm.mainHeader);
    await performAction('readDocumentsSubmit');
  });
});
