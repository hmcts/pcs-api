import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test, { } from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import { checkYourAnswersEnterCounterClaim, counterClaimAmount, courtPermission, typeOfCounterClaim } from '@data/page-data-figma/page-data-caseManagement-figma';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagement.action';

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {

  await test.step(`Clear all cookies and initialize the test executors`, async () => {
    await context.clearCookies();
    initializeExecutor(page);
    initializeCMExecutor(page);
  });

  await test.step('Create and submit case, update payment and retrieve party details', async () => {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
    await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
    await performAction('updatePaymentAPI');
    await performAction('getAllPartyDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseFileView.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadCaseFileView.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayloadCaseFileView
    });
  })

  await test.step(`Login as Hearing Center Admin - ${user.hearingCenterAdmin.email} and navigate to Case Summary page`, async () => {
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await dismissCookieBanner(page, 'additional');
    await performAction('login', user.hearingCenterAdmin);
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToSummaryPage');
  });
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
    await test.step(`Select ${caseSummary.counterClaim} from the next event drop and go`, async () => {
      await performAction('selectAnEvent', { eventType: caseSummary.counterClaim, nextPage: courtPermission.mainHeader });
    });
    await test.step(`Validate court permission page displays mandatory field errors`, async () => {
      await performAction('errorValidationCourtPermissionPage', courtPermission.errorValidation);
    });
    await test.step(`Provide court permission and counterclaim submission details`, async () => {
      await performAction('addCourtPermissionDetails', {
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
    });
    await test.step(`Validate Type of Counter Claim page displays mandatory field errors`, async () => {
      await performAction('errorValidationTypeOfCounterClaimPage', typeOfCounterClaim.errorValidation);
    })
    await test.step(`Select Type of Counter claim in this page`, async () => {
      await performAction('selectCounterClaimType', {
        question: typeOfCounterClaim.typeOfCounterClaimQuestion,
        option: typeOfCounterClaim.sumOfMoneyRadioOption,
        nextPage: counterClaimAmount.mainHeader
      })
    });
    await test.step(`Validate Check your answers page`, async () => {
      await performAction('reTryOnCallBackError', counterClaimAmount.continueButton, checkYourAnswersEnterCounterClaim.mainHeader);
    });
  });

  test('Case management - Counter Claim No Court Permission @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(courtPermission.dateTypeGrantPermissionHiddenUserInput as string);
    let date1 = CaseManagementCommonUtils.getRandomDate(courtPermission.dateTypeCCReceivedHiddenUserInput as string);
    let party = allPartyDetails[0];
    await test.step(`Select ${caseSummary.counterClaim} from the next event drop and go`, async () => {
      await performAction('selectAnEvent', { eventType: caseSummary.counterClaim, nextPage: courtPermission.mainHeader });
    });
    await test.step(`Provide court permission and counterclaim submission details`, async () => {
      await performAction('addCourtPermissionDetails', {
        question: courtPermission.hasTheCOurtGivenPermissionQuestion,
        option: courtPermission.noRadioOption,
        grantPermissionLabel: courtPermission.grantPermissionHiddenLabel,
        permissionDate: date,
        question1: courtPermission.partySubmittedCCHiddenQuestion,
        option1: party,
        ccReceivedLabel: courtPermission.ccReceivedDateHiddenLabel,
        ccReceivedDate: date1,
        nextPage: typeOfCounterClaim.mainHeader
      })
    });
    await test.step(`Select Type of Counter claim in this page`, async () => {
      await performAction('selectCounterClaimType', {
        question: typeOfCounterClaim.typeOfCounterClaimQuestion,
        option: typeOfCounterClaim.sumOfMoneyRadioOption,
        nextPage: counterClaimAmount.mainHeader
      })
    });
    await test.step(`Validate Check your answers page`, async () => {
      await performAction('reTryOnCallBackError', counterClaimAmount.continueButton, checkYourAnswersEnterCounterClaim.mainHeader);
    });
  })
});