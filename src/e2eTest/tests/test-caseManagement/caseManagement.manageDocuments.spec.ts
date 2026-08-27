import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
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

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
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
  test('Case management - Manage documents - Amend @CM', async () => {
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
    await performAction('confirmAmend', { fileName: fileName, party: party, fileDate: date, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Amend');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Applications',
      submitPayload: makeAnApplicationApiData.makeAnApplicationAdjournPayload(defendantUserDetails[0].id, defendantUserDetails[0].name),
      caseWorkerAmend: CaseManagementCommonUtils.renameDocument(fileName, date, appType)
    });
  });

  test('Case management - Manage documents - Amend Document not related to any App or Counterclaim @CM', async () => {
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
    await performAction('confirmAmend', { fileName: fileName, party: party, fileDate: date, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Amend');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Evidence',
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView,
      caseWorkerAmend: CaseManagementCommonUtils.renameDocument(fileName, date)
    });
  });

  test('Case management - Manage documents - Amend Document without any Issue date @CM', async () => {
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
    await performAction('confirmAmend', { fileName: fileName, party: party, fileDate: date, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Amend');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Uncategorised documents',
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView,
      caseWorkerAmend: CaseManagementCommonUtils.renameDocument(fileName)
    });
  });

  test('Case management - Manage documents - Upload @CM', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(uploadADocument.dateTypeHiddenUserInput);
    let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[0];
    let party = allPartyDetails[0]
    let fileName = uploadADocument.uploadDocHiddenOption[0];
    await performAction('selectAnEvent', { eventType: caseSummary.manageDocuments.upload });
    await performValidation('mainHeader', uploadADocument.mainHeader);
    await performAction('errorValidationUploadADocumentPage', uploadADocument.errorValidation);
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
    await performAction('confirmUpload', { fileName: fileName, app: appType, party: party, fileDate: date, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Upload');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Applications',
      submitPayload: makeAnApplicationApiData.makeAnApplicationAdjournPayload(defendantUserDetails[0].id, defendantUserDetails[0].name),
      caseWorkerUpload: CaseManagementCommonUtils.renameDocument(fileName, date, appType)
    });
  });

  test('Case management - Manage documents - Upload Document not related to any App or Counterclaim @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(uploadADocument.dateTypeHiddenUserInput);
    let appType = uploadADocument.notRelatedToAppRadioOption;
    let party = allPartyDetails[1];
    let fileName = uploadADocument.uploadDocHiddenOption[1];
    await performAction('selectAnEvent', { eventType: caseSummary.manageDocuments.upload });
    await performValidation('mainHeader', uploadADocument.mainHeader);
    await performAction('uploadADocument', { label: uploadADocument.uploadADocumentTextLabel, file: fileName })
    await performAction('selectDynamicAppAndPartyDocRelatedTo', {
        question: uploadADocument.whichAppOrCounterClaimThisRelateToQuestion,
        option: appType,
        label: uploadADocument.addIssueDateTextLabel,
        date: date,
        question1: uploadADocument.partyDocRelatedToQuestion,
        option1: party,
        dropQn: uploadADocument.whichTypeOfDocHiddenQuestion,
        selectOption: uploadADocument.whichTypeHiddenOption[0],
        nextPage: checkYourAnswersUploadADocument.mainHeader
    });
    await performAction('clickButton', checkYourAnswersUploadADocument.submitButton);
    await performAction('confirmUpload', { fileName: fileName, app: appType, party: party, fileDate: date, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView, });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Upload');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Property documents',
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView,
      caseWorkerUpload: CaseManagementCommonUtils.renameDocument(fileName, date)
    });
  });

  test('Case management - Manage documents - Upload Document without any Issue date @CM', async () => {
    let date = '';
    let appType = uploadADocument.notRelatedToAppRadioOption;
    let party = allPartyDetails[2];
    let fileName = uploadADocument.uploadDocHiddenOption[1];
    await performAction('selectAnEvent', { eventType: caseSummary.manageDocuments.upload });
    await performValidation('mainHeader', uploadADocument.mainHeader);
    await performAction('uploadADocument', { label: uploadADocument.uploadADocumentTextLabel, file: fileName })
    await performAction('selectDynamicAppAndPartyDocRelatedTo', {
      question: uploadADocument.whichAppOrCounterClaimThisRelateToQuestion,
      option: appType,
      label: uploadADocument.addIssueDateTextLabel,
      date: date,
      question1: uploadADocument.partyDocRelatedToQuestion,
      option1: party,
      dropQn: uploadADocument.whichTypeOfDocHiddenQuestion,
      selectOption: uploadADocument.whichTypeHiddenOption[1],
      nextPage: checkYourAnswersUploadADocument.mainHeader
    });
    await performAction('clickButton', checkYourAnswersUploadADocument.submitButton);
    await performAction('confirmUpload', { fileName: fileName, app: appType, party: party, fileDate: date, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView, });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage documents: Upload');
    await performAction('clickTab', home.caseFileView);
    await performAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Evidence',
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView,
      caseWorkerUpload: CaseManagementCommonUtils.renameDocument(fileName)
    });
  });
});