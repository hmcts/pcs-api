import { createCaseApiData, makeAnApplicationApiData } from '@data/api-data';
import { initializeExecutor, performValidation } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import { amendDocumentDetails, checkYourAnswersAmendDocument, checkYourAnswersUploadADocument, selectDocument, uploadADocument } from '@data/page-data-figma/page-data-caseManagement-figma';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagement.action';
import { createCaseApiWalesData } from '@data/api-data/createCaseWales.api.data';
import { submitCaseApiDataWales } from '@data/api-data/submitCaseWales.api.data';

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiWalesData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiDataWales.submitCasePayloadCaseFileView });
  await performAction('getAddressInfo', { data: createCaseApiWalesData.createCasePayload });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('getAllPartyDetails', {
    defendant1NameKnown: submitCaseApiDataWales.submitCasePayloadCaseFileView.defendant1.nameKnown,
    additionalDefendants: submitCaseApiDataWales.submitCasePayloadCaseFileView.addAnotherDefendant,
    payLoad: submitCaseApiDataWales.submitCasePayloadCaseFileView
  });

  for (const defendant of defendantUserDetails) {
    await performAction('makeAnApplicationAPI', {
      data: makeAnApplicationApiData.makeAnApplicationAdjournWalesPayload(
        defendant.id,
        defendant.name
      ),
    });
  };
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.hearingCenterAdminWales);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');

});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Case management - Manage documents Wales Journey @nightly', async () => {
  test('Case management - Manage documents - Amend Wales Journey @CM @regression', async () => {
      let date = CaseManagementCommonUtils.getRandomDate(uploadADocument.dateTypeHiddenUserInput);
      let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[0];
      let party = allPartyDetails[0];
      let fileName = (selectDocument.typeOfDocumentHiddenRadioOption)[0].split('-')[0].trim();
      await performAction('selectAnEvent', { eventType: caseSummary.manageDocuments.amend });
      await performValidation('mainHeader', selectDocument.mainHeader);
      await performAction('errorValidationSelectDocumentPage', selectDocument.errorValidation);
      await performAction('selectDocumentToAmend', {
        question: selectDocument.whichFolderQuestion, option: (selectDocument.docFolderHiddenOption)[0],
        question1: selectDocument.documentToAmendHiddenQuestion, option1: (selectDocument.typeOfDocumentHiddenRadioOption)[0],
        nextPage: amendDocumentDetails.mainHeader
      });
      await performAction('inputText', amendDocumentDetails.fileNameInputTextLabel, fileName);
      await performAction('selectDynamicAppAndPartyDocRelatedTo', {
        question: amendDocumentDetails.whichAppOrCounterClaimThisRelateToQuestion,
        option: appType,
        label: amendDocumentDetails.addIssueDateTextLabel,
        date: date,
        question1: amendDocumentDetails.partyDocRelatedToQuestion,
        option1: party,
        nextPage: checkYourAnswersAmendDocument.mainHeader
      });
      await performAction('clickButton', checkYourAnswersAmendDocument.submitButton);
      await performAction('confirmAmend', { fileName: fileName, party: party, fileDate: date, submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView });
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Amend');
      await performAction('clickTab', home.caseFileView);
      await performAction('validateCaseFileViewFolders', home.caseFileFolders);
      await performAction('validateCaseFileViewIndividualFolder', {
        folder: 'Applications',
        submitPayload: makeAnApplicationApiData.makeAnApplicationAdjournWalesPayload(defendantUserDetails[0].id, defendantUserDetails[0].name),
        caseWorkerAmend: CaseManagementCommonUtils.renameDocument(fileName, date, appType)
      });
    });
  
    test('Case management - Manage documents - Amend Document not related to any App or Counterclaim Wales Journey @CM', async () => {
      let date = CaseManagementCommonUtils.getRandomDate(uploadADocument.dateTypeHiddenUserInput);
      let appType = amendDocumentDetails.notRelatedToAppRadioOption;
      let party = allPartyDetails[1];
      let fileName = (selectDocument.typeOfDocumentHiddenRadioOption)[2].split('-')[0].trim();
      await performAction('selectAnEvent', { eventType: caseSummary.manageDocuments.amend });
      await performValidation('mainHeader', selectDocument.mainHeader);
      await performAction('selectDocumentToAmend', {
        question: selectDocument.whichFolderQuestion, option: (selectDocument.docFolderHiddenOption)[2],
        question1: selectDocument.documentToAmendHiddenQuestion, option1: (selectDocument.typeOfDocumentHiddenRadioOption)[2],
        nextPage: amendDocumentDetails.mainHeader
      });
      await performAction('inputText', amendDocumentDetails.fileNameInputTextLabel, fileName);
      await performAction('selectDynamicAppAndPartyDocRelatedTo', {
        question: amendDocumentDetails.whichAppOrCounterClaimThisRelateToQuestion,
        option: appType,
        label: amendDocumentDetails.addIssueDateTextLabel,
        date: date,
        question1: amendDocumentDetails.partyDocRelatedToQuestion,
        option1: party,
        dropQn: amendDocumentDetails.whichTypeOfDocHiddenQuestion,
        selectOption: (amendDocumentDetails.whichTypeHiddenOption)[2],
        nextPage: checkYourAnswersAmendDocument.mainHeader
      });
      await performAction('clickButton', checkYourAnswersAmendDocument.submitButton);
      await performAction('confirmAmend', { fileName: fileName, party: party, fileDate: date, submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView });
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Amend');
      await performAction('clickTab', home.caseFileView);
      await performAction('validateCaseFileViewFolders', home.caseFileFolders);
      await performAction('validateCaseFileViewIndividualFolder', {
        folder: 'Evidence',
        submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView,
        caseWorkerAmend: CaseManagementCommonUtils.renameDocument(fileName, date)
      });
    });
  
    test('Case management - Manage documents - Amend Document without any Issue date Wales Journey @CM', async () => {
      let date = '';
      let appType = amendDocumentDetails.notRelatedToAppRadioOption;
      let party = allPartyDetails[0];
      let fileName = (selectDocument.typeOfDocumentHiddenRadioOption)[1].split('-')[0].trim();
      await performAction('selectAnEvent', { eventType: caseSummary.manageDocuments.amend });
      await performValidation('mainHeader', selectDocument.mainHeader);
      await performAction('selectDocumentToAmend', {
        question: selectDocument.whichFolderQuestion, option: (selectDocument.docFolderHiddenOption)[1],
        question1: selectDocument.documentToAmendHiddenQuestion, option1: (selectDocument.typeOfDocumentHiddenRadioOption)[1],
        nextPage: amendDocumentDetails.mainHeader
      });
      await performAction('inputText', amendDocumentDetails.fileNameInputTextLabel, fileName);
      await performAction('selectDynamicAppAndPartyDocRelatedTo', {
        question: amendDocumentDetails.whichAppOrCounterClaimThisRelateToQuestion,
        option: appType,
        label: amendDocumentDetails.addIssueDateTextLabel,
        date: date,
        question1: amendDocumentDetails.partyDocRelatedToQuestion,
        option1: party,
        dropQn: amendDocumentDetails.whichTypeOfDocHiddenQuestion,
        selectOption: (amendDocumentDetails.whichTypeHiddenOption)[3],
        nextPage: checkYourAnswersAmendDocument.mainHeader
      });
      await performAction('clickButton', checkYourAnswersAmendDocument.submitButton);
      await performAction('confirmAmend', { fileName: fileName, party: party, fileDate: date, submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView });
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Amend');
      await performAction('clickTab', home.caseFileView);
      await performAction('validateCaseFileViewFolders', home.caseFileFolders);
      await performAction('validateCaseFileViewIndividualFolder', {
        folder: 'Uncategorised documents',
        submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView,
        caseWorkerAmend: CaseManagementCommonUtils.renameDocument(fileName)
      });
    });
    
  test('Case management - Manage documents - Upload Wales Journey @CM', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(uploadADocument.dateTypeHiddenUserInput);    
    let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[1];
    let party = allPartyDetails[1];
    let fileName = uploadADocument.uploadDocHiddenOption[1];
    await performAction('selectAnEvent', { eventType: caseSummary.manageDocuments.upload });
    await performValidation('mainHeader', uploadADocument.mainHeader);
    // await performAction('errorValidationSelectDocumentPage', selectDocument.errorValidation);
    await performAction('uploadADocument', { label: uploadADocument.uploadADocumentTextLabel, file: fileName })
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
    await performAction('confirmUpload', { fileName: fileName, app: appType, party: party, fileDate: date, submitPayload: submitCaseApiDataWales.submitCasePayloadCaseFileView, });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Upload');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Applications',
      submitPayload: makeAnApplicationApiData.makeAnApplicationAdjournWalesPayload(defendantUserDetails[1].id,defendantUserDetails[1].name),
      caseWorkerUpload: CaseManagementCommonUtils.renameDocument(fileName, date, appType)
    });

  });
});
