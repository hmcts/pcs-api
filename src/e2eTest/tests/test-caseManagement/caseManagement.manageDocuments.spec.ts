import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performValidation } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import { amendDocumentDetails, checkYourAnswersUploadADocument, selectDocument, uploadADocument } from '@data/page-data-figma/page-data-caseManagement-figma';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagement.action';

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('getAllPartyDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseFileView.defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayloadCaseFileView.addAnotherDefendant,
    payLoad: submitCaseApiData.submitCasePayloadCaseFileView
  });

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
  await performAction('login', user.hearingCenterAdmin);
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
    await performAction('selectDocumentToAmend', {
      question: selectDocument.whichFolderQuestion, option: selectDocument.docFolderHiddenOption,
      question1: selectDocument.documentToAmendHiddenQuestion, option1: selectDocument.typeOfDocumentHiddenRadioOption,
      nextPage: amendDocumentDetails.mainHeader
    });
  });

  test('Case management - Manage documents - Upload @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(uploadADocument.dateTypeHiddenUserInput);    
    let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[1];
    let party = allPartyDetails[1]
    await performAction('selectAnEvent', { eventType: caseSummary.manageDocuments.upload });
    await performValidation('mainHeader', uploadADocument.mainHeader);
    // await performAction('errorValidationSelectDocumentPage', selectDocument.errorValidation);
    await performAction('uploadADocument', { label: uploadADocument.uploadADocumentTextLabel, file: 'legalAidCertificate.pdf' })
    await performAction('selectDynamicAppAndPartyDocRelatedTo', {
      question: uploadADocument.whichAppOrCounterClaimThisRelateToQuestion,
      option: appType,
      label: uploadADocument.addIssueDateTextLabel,
      date: date,
      question1: uploadADocument.partyDocRelatedToQuestion,
      option1: party,
      nextPage: checkYourAnswersUploadADocument.mainHeader
    });
    await performAction('clickButton', checkYourAnswersUploadADocument.submitButton);
    await performAction('confirmUpload', { fileName: 'legalAidCertificate.pdf', app: appType, party: party, fileDate: date, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView, });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Upload');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Applications',
      submitPayload: makeAnApplicationApiData.makeAnApplicationAdjournPayload(defendantUserDetails[1].id,defendantUserDetails[1].name),
      caseWorkerUpload: CaseManagementCommonUtils.renameDocument('legalAidCertificate.pdf', appType, party, date)
    });

  });
});
