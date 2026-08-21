import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performValidation } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import { addParty, checkYourAnswersManageParties, manageParty, partyDetails, selectDocument, uploadADocument } from '@data/page-data-figma/page-data-caseManagement-figma';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';
import { addressInfo, allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagement.action';

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

test.describe('Case management - Case Party Management e2e Journey @nightly', async () => {
  test('Case management - Add a Party to the Case - Defendant @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(partyDetails.dateTypeHiddenUserInput);
    let firstName = partyDetails.firstNames[Math.floor(Math.random() * partyDetails.firstNames.length)];
    let lastName = partyDetails.lastNames[Math.floor(Math.random() * partyDetails.lastNames.length)];
    await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
    await performValidation('mainHeader', manageParty.mainHeader);
    await performAction('selectManageParty', {
      partyToChangeQn: manageParty.whatChangeQuestion,
      option: manageParty.addPartyRadioOption,
      whichPartyQn: manageParty.typeOfPartyHiddenQuestion,
      option1: manageParty.defendantHiddenRadioOption,
      nextPage: partyDetails.mainHeader,
    });
    await performAction('addNewParty', {
      label1: partyDetails.firstNameTextLabel,
      input1: firstName,
      label2: partyDetails.lastNameTextLabel,
      input2: lastName,
      dateLabel: partyDetails.addDOBHiddenTextLabel,
      date: date,
    });
    await performAction('addNewPartyAddress', {
      enterUKPostcodeTextLabel: partyDetails.enterUKPostcodeTextLabel,
      postcode: addressInfo.engOrWalPostcode,
      findAddressButton: partyDetails.findAddressButton,
      addressSelectLabel: partyDetails.addressSelectHiddenLabel,
      addressIndex: partyDetails.addressIndex,
      nextPage: checkYourAnswersManageParties.mainHeader
    });
    await performAction('clickButton', checkYourAnswersManageParties.submitButton);
    await performAction('confirmAddParty', { userType: `Defendant`, name: `${firstName} ${lastName}`, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('mainHeader', home.caseParties);
    await performAction('validateDefendantDetails', {
      firstName: firstName,
      lastName: lastName,
      mainTable: 'Additional defendant 3',
      subTable: 'Service address'
    });
  });

  test('Case management - Add a Party to the Case - Claimant @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(partyDetails.dateTypeHiddenUserInput);
    let firstName = partyDetails.firstNames[Math.floor(Math.random() * partyDetails.firstNames.length)];
    let lastName = partyDetails.lastNames[Math.floor(Math.random() * partyDetails.lastNames.length)];
    let orgName = partyDetails.orgNames[Math.floor(Math.random() * partyDetails.orgNames.length)];
    await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
    await performValidation('mainHeader', manageParty.mainHeader);
    await performAction('selectManageParty', {
      partyToChangeQn: manageParty.whatChangeQuestion,
      option: manageParty.addPartyRadioOption,
      whichPartyQn: manageParty.typeOfPartyHiddenQuestion,
      option1: manageParty.claimantHiddenRadioOption,
      nextPage: partyDetails.mainHeader,
    });
    await performAction('addNewParty', {
      orgLabel: partyDetails.orgNameHiddenTextLabel,
      orgInput: orgName,
      label1: partyDetails.firstNameTextLabel,
      input1: firstName,
      label2: partyDetails.lastNameTextLabel,
      input2: lastName,
      dateLabel: partyDetails.addDOBHiddenTextLabel,
      date: date,
    });
    await performAction('addNewPartyAddress', {
      enterUKPostcodeTextLabel: partyDetails.enterUKPostcodeTextLabel,
      postcode: addressInfo.engOrWalPostcode,
      findAddressButton: partyDetails.findAddressButton,
      addressSelectLabel: partyDetails.addressSelectHiddenLabel,
      addressIndex: partyDetails.addressIndex,
      nextPage: checkYourAnswersManageParties.mainHeader
    });
    await performAction('clickButton', checkYourAnswersManageParties.submitButton);
    await performAction('confirmAddParty', { userType: `Claimant`, name: `${firstName} ${lastName}`, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('mainHeader', home.caseParties);
    await performAction('validateClaimantDetails', {
      orgName: orgName,
      email: partyDetails.emailHiddenTextInput,
      phone: partyDetails.phoneHiddenTextInput,
      table: 'Additional claimant 1'
    });
  });

  test('Case management - Add a Party to the Case - Litigation friend @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(partyDetails.dateTypeHiddenUserInput);
    let firstName = partyDetails.firstNames[Math.floor(Math.random() * partyDetails.firstNames.length)];
    let lastName = partyDetails.lastNames[Math.floor(Math.random() * partyDetails.lastNames.length)];
    let orgName = partyDetails.orgNames[Math.floor(Math.random() * partyDetails.orgNames.length)];
    let party = allPartyDetails[1];
    await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
    await performValidation('mainHeader', manageParty.mainHeader);
    await performAction('selectManageParty', {
      partyToChangeQn: manageParty.whatChangeQuestion,
      option: manageParty.addPartyRadioOption,
      whichPartyQn: manageParty.typeOfPartyHiddenQuestion,
      option1: manageParty.litigationFriendHiddenRadioOption,
      nextPage: addParty.mainHeader,
    });
    await performAction('clickRadioButton', { question: addParty.litigationFriendQuestion, option: party });
    await performAction('reTryOnCallBackError', addParty.continueButton, partyDetails.mainHeader as string);
    await performAction('addNewParty', {
      orgLabel: partyDetails.orgNameHiddenTextLabel,
      orgInput: orgName,
      label1: partyDetails.firstNameTextLabel,
      input1: firstName,
      label2: partyDetails.lastNameTextLabel,
      input2: lastName,
      dateLabel: partyDetails.addDOBHiddenTextLabel,
      date: date,
    });
    await performAction('addNewPartyAddress', {
      enterUKPostcodeTextLabel: partyDetails.enterUKPostcodeTextLabel,
      postcode: addressInfo.engOrWalPostcode,
      findAddressButton: partyDetails.findAddressButton,
      addressSelectLabel: partyDetails.addressSelectHiddenLabel,
      addressIndex: partyDetails.addressIndex,
      nextPage: checkYourAnswersManageParties.mainHeader
    });
    await performAction('clickButton', checkYourAnswersManageParties.submitButton);
    await performAction('confirmAddParty', { userType: `Litigation friend`, name: `${firstName} ${lastName}`, submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('mainHeader', home.caseParties);
    await performAction('validateDefendantDetails', {
      firstName: firstName,
      lastName: lastName,
      actingFor: party,
      mainTable: 'Litigation friend',
      subTable: 'Service address'
    });
  });
});