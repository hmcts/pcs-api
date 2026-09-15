import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performValidation } from '@utils/controller';
import test, { BrowserContext, expect, Page } from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import { amendDocumentDetails, checkYourAnswersAmendDocument, checkYourAnswersUploadADocument, selectDocument, uploadADocument } from '@data/page-data-figma/page-data-caseManagement-figma';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagement.action';
import { getCaseTypeId } from '@utils/common/caseType.utils';

test.use({ storageState: undefined })

let genAppPayload: (id: string, name: string) => any;

test.beforeEach(async ({ page, context }, testInfo) => {
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

  genAppPayload =
    testInfo.title.includes('WithOut_Notice')
      ? makeAnApplicationApiData.makeAnApplicationAdjournWithOutNoticePayload
      : makeAnApplicationApiData.makeAnApplicationAdjournPayload

  for (const defendant of defendantUserDetails) {
    await performAction('makeAnApplicationAPI', {
      data: genAppPayload(
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
      submitPayload: genAppPayload(defendantUserDetails[0].id, defendantUserDetails[0].name),
      caseWorkerAmend: CaseManagementCommonUtils.renameDocument(fileName, date, appType)
    });
  });

  test('Case management - Manage documents - Amend Document not related to any App or Counterclaim @CM @regression', async () => {
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

  test('Case management - Manage documents - Upload @CM @regression', async ({ page, context }) => {
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
      submitPayload: genAppPayload(defendantUserDetails[0].id, defendantUserDetails[0].name),
      caseWorkerUpload: CaseManagementCommonUtils.renameDocument(fileName, date, appType)
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
      folder: 'Applications',
      submitPayload: genAppPayload(defendantUserDetails[0].id, defendantUserDetails[0].name),
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

  test('Case management - Manage documents - Upload Document without any Issue date @CM @regression', async ({ page, context }) => {
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
      folder: 'Evidence',
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView,
      caseWorkerUpload: CaseManagementCommonUtils.renameDocument(fileName)
    });
  });

  test('Case management - Manage documents - Upload Document Journey WithOut_Notice @CM @regression', async ({ page, context }) => {
    let date = CaseManagementCommonUtils.getRandomDate(uploadADocument.dateTypeHiddenUserInput);
    let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[0];
    let party = allPartyDetails[1];
    let fileName = uploadADocument.uploadDocHiddenOption[3];
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
      folder: 'Applications',
      submitPayload: genAppPayload(defendantUserDetails[0].id, defendantUserDetails[0].name),
      caseWorkerUpload: CaseManagementCommonUtils.renameDocument(fileName, date, appType)
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
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView,
    });
    await performAction('validateCaseFileViewIndividualFolder', {
      folder: 'Applications',
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView,
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
