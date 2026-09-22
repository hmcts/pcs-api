import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performValidation } from '@utils/controller';
import test, { BrowserContext, expect, Page } from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import { amendDocumentDetails, checkYourAnswersAmendDocument, checkYourAnswersEnterCounterClaim, checkYourAnswersUploadADocument, counterClaimAmount, courtPermission, selectDocument, typeOfCounterClaim, uploadADocument } from '@data/page-data-figma/page-data-caseManagement-figma';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagement.action';
import { getCaseTypeId } from '@utils/common/caseType.utils';

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

test.describe('Case management - Counter Claim e2e Journey @nightly', async () => {
  test('Case management - Counter Claim @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(courtPermission.dateTypeGrantPermissionHiddenUserInput as string);
    let date1 = CaseManagementCommonUtils.getRandomDate(courtPermission.dateTypeCCReceivedHiddenUserInput as string);
    let party = allPartyDetails[0];
    await performAction('selectAnEvent', { eventType: caseSummary.counterClaim , nextPage: courtPermission.mainHeader});
    await performAction('addCourtPermissionDetails',{
      question: courtPermission.hasTheCOurtGivenPermissionQuestion,
      option: courtPermission.yesRadioOption,
      grantPermissionLabel: courtPermission.grantPermissionHiddenLabel,
      permissionDate: date,      
      question1: courtPermission.partySubmittedCCHiddenQuestion,
      option1: party,
      ccReceivedLabel: courtPermission.ccReceivedDateHiddenLabel,
      ccReceivedDate: date1,
      nextPage: typeOfCounterClaim.mainHeader      
    })
    await performAction('selectCounterClaimType',{
      question: typeOfCounterClaim.typeOfCounterClaimQuestion,
      option: typeOfCounterClaim.sumOfMoneyRadioOption,
      nextPage: counterClaimAmount.mainHeader
    })
    await performAction('reTryOnCallBackError',counterClaimAmount.continueButton,checkYourAnswersEnterCounterClaim.mainHeader);
  });
});