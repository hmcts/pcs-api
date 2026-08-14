import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performValidation } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import {
  checkYourAnswersUpdatePartyDetails,
  manageParties,
  updatePartyDetails,
} from '@data/page-data-figma/page-data-caseManagement-figma';
import {
  CaseManagementCommonUtils
} from "@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action";

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
  console.log(`Case created with case number: ${process.env.CASE_NUMBER}`);
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');

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

test.describe('Case management - Case Worker Manage Parties @nightly', async () => {
  test('Case management - Case Worker update party- Defendants details @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(updatePartyDetails.dateTypeHiddenUserInput);
    await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
    await performValidation('mainHeader', manageParties.mainHeader);
    await performAction('selectParty',{
      question1: manageParties.whatChangeYouWantMakeQuestion,
      option1: manageParties.updatePartyRadioOption,
      question2: manageParties.whichPartyContactInformationHiddenQuestion,
      option2:manageParties.defendant1HiddenRadioOption,
      nextPage: updatePartyDetails.mainHeader
    });
    await performAction('updatePartyDetails', {
      DOBLabel: updatePartyDetails.dateOfBirthHiddenLabel,
      date: date,
      enterUKPostcodeTextLabel: updatePartyDetails.enterUKPostcodeTextLabel,
      postcode: updatePartyDetails.englandPostCodeTextInput,
      button: updatePartyDetails.findAddressButton,
      addressSelectLabel: updatePartyDetails.addressSelectHiddenLabel,
      addressIndex: updatePartyDetails.defendantAddressIndex,
      nextPage: checkYourAnswersUpdatePartyDetails.mainHeader
    });
    await performAction('clickButton', checkYourAnswersUpdatePartyDetails.submitButton);
    await performAction('confirmPartyDetailsUpdated', { userType: `Defendant's details`, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage parties');
  });

  test('Case management - Case Worker update party - Claimant details @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(updatePartyDetails.dateTypeHiddenUserInput);
    await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
    await performValidation('mainHeader', manageParties.mainHeader);
    await performAction('selectParty',{
      question1: manageParties.whatChangeYouWantMakeQuestion,
      option1: manageParties.updatePartyRadioOption,
      question2: manageParties.whichPartyContactInformationHiddenQuestion,
      option2:manageParties.ClaimantHiddenRadioOption,
      nextPage: updatePartyDetails.mainHeader
    });
    await performAction('updatePartyDetails', {
      DOBLabel: updatePartyDetails.dateOfBirthHiddenLabel,
      date: date,
      enterUKPostcodeTextLabel: updatePartyDetails.enterUKPostcodeTextLabel,
      postcode: updatePartyDetails.englandPostCodeTextInput,
      button: updatePartyDetails.findAddressButton,
      addressSelectLabel: updatePartyDetails.addressSelectHiddenLabel,
      addressIndex: updatePartyDetails.claimantAddressIndex,
      nextPage: checkYourAnswersUpdatePartyDetails.mainHeader
    });
    await performAction('clickButton', checkYourAnswersUpdatePartyDetails.submitButton);
    await performAction('confirmPartyDetailsUpdated', { userType: `Claimant's details`, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage parties');
  });
});
