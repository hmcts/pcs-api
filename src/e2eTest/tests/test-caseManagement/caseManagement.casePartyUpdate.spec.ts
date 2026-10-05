import { createCaseApiData, makeAnApplicationApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performValidation } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction } from '@utils/controller-caseManagement';
import { addParty, checkYourAnswersManageParties, manageParty, partyDetails, selectDocument, uploadADocument, updatePartyDetails, checkFullPartyDetails } from '@data/page-data-figma/page-data-caseManagement-figma';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';
import { addressInfo, allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagement.action';
import { getDefendantAddress, getDefendantAddressByName } from '@utils/common/userData.utils';

test.use({ storageState: undefined })
let createPayload: Record<string, any>;
let submitPayload: Record<string, any>;

test.beforeEach(async ({ page, context }, testInfo) => {
  await test.step(`Clear all cookies and initialize the test executors`, async () => {
    await context.clearCookies();
    initializeExecutor(page);
    initializeCMExecutor(page);
  });

  createPayload = createCaseApiData.createCasePayload
  submitPayload = testInfo.title.toLowerCase().includes('single defendant')
    ? submitCaseApiData.submitCasePayloadDefault
    : submitCaseApiData.submitCasePayloadCaseFileView;

  await test.step('Create and submit case, update payment and retrieve party details', async () => {
    await performAction('createCaseAPI', { data: createPayload });
    await performAction('submitCaseAPI', { data: submitPayload });
    await performAction('getAddressInfo', { data: createPayload });
    await performAction('updatePaymentAPI');
    await performAction('getAllPartyDetails', {
      defendant1NameKnown: submitPayload.defendant1.nameKnown,
      additionalDefendants: submitPayload.addAnotherDefendant,
      payLoad: submitPayload
    });
  });

  if (testInfo.title.includes('GENAPP')) {
    await performAction('getCaseAPI', 'Link Solicitor');
    for (const defendant of defendantUserDetails) {
      await performAction('makeAnApplicationAPI', {
        data: makeAnApplicationApiData.makeAnApplicationAdjournPayload(
          defendant.id,
          defendant.name
        ),
      });
    };
  };

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

test.describe('Case management - Case Party Management e2e Journey @nightly', async () => {
  test(`Case management - As a Case Worker Add a 'Defendant' to the Case @CM @regression`, async () => {
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
    await performAction('confirmAddParty', {
      userType: `Defendant`,
      name: `${firstName} ${lastName}`,
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView
    });
    await performValidation('mainHeader', home.caseParties);
    await performAction('validateDefendantDetails', {
      firstName: firstName,
      lastName: lastName,
      mainTable: 'Additional defendant 3',
      subTable: 'Service address',
      defAddress: getDefendantAddress(submitPayload, createPayload, `${firstName} ${lastName}`),
    });
  });

  test(`Case management - As a Case Worker Add a 'Claimant' to the Case @CM @regression`, async () => {
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
    await performAction('confirmAddParty', {
      userType: `Claimant`,
      name: `${firstName} ${lastName}`,
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView
    });
    await performValidation('mainHeader', home.caseParties);
    await performAction('validateClaimantDetails', {
      orgName: orgName,
      email: partyDetails.emailHiddenTextInput,
      phone: partyDetails.phoneHiddenTextInput,
      table: 'Additional claimant 1'
    });
  });

  test(`Case management - As a Case Worker Add a 'Litigation Friend' to the Case @CM @regression`, async () => {
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
    await performAction('confirmAddParty', {
      userType: `Litigation friend`,
      name: `${firstName} ${lastName}`,
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView
    });
    await performValidation('mainHeader', home.caseParties);
    await performAction('validateDefendantDetails', {
      firstName: firstName,
      lastName: lastName,
      actingFor: party,
      mainTable: 'Litigation friend',
      subTable: 'Service address',
      defAddress: getDefendantAddress(submitPayload, createPayload, `${firstName} ${lastName}`),
    });
  });

  test(`Case management - As a Case Worker Update 'Defendant' details @CM @regression`, async () => {
    let date = CaseManagementCommonUtils.getRandomDate(updatePartyDetails.dateTypeHiddenUserInput);
    let party = allPartyDetails[1];
    let name = party.split(' - ')[0];
    let partyAddress = getDefendantAddressByName(submitPayload, createPayload, name);
    let removeParty = `${checkFullPartyDetails.doYouWantToRemoveHiddenQuestion} ${party}?`;
    await test.step(`Select event "Manage Parties" from event drop down`, async () => {
      await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
      await performValidation('mainHeader', manageParty.mainHeader);
    });
    await test.step(`Select the party to be updated`, async () => {
      await performAction('selectParty', {
        question1: manageParty.whatChangeQuestion,
        option1: manageParty.updatePartyRadioOption,
        question2: manageParty.whichPartyContactInformationHiddenQuestion,
        option2: party,
        nextPage: updatePartyDetails.mainHeader
      });
    });
    await test.step(`Update the parties Date of Birth and contact details`, async () => {
      await performAction('updatePartyDetails', {
        DOBLabel: updatePartyDetails.dateOfBirthHiddenLabel,
        date: date,
        enterUKPostcodeTextLabel: updatePartyDetails.enterUKPostcodeTextLabel,
        postcode: updatePartyDetails.englandPostCodeTextInput,
        button: updatePartyDetails.findAddressButton,
        addressSelectLabel: updatePartyDetails.addressSelectHiddenLabel,
        addressIndex: updatePartyDetails.defendantAddressIndex,
        nextPage: checkYourAnswersManageParties.mainHeader
      });
    });
    await test.step(`Validated the answers provided and Confirm if update is successful`, async () => {
      await performAction('clickButton', checkYourAnswersManageParties.submitButton);
      await performAction('confirmPartyDetailsUpdated', {
        userType: `Defendant's details`,
        submitPayload: submitCaseApiData.submitCasePayloadCaseFileView
      });
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage parties');
    });
    await test.step(`Select event "Manage Parties" from event drop down`, async () => {
      await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
      await performValidation('mainHeader', manageParty.mainHeader);
    });
    await test.step(`Select the party to be removed`, async () => {
      await performAction('selectParty', {
        question1: manageParty.whatChangeQuestion,
        option1: manageParty.removePartyRadioOption,
        question2: manageParty.whichPartyYouRemovingHiddenQuestion,
        option2: party,
        nextPage: checkFullPartyDetails.mainHeader
      });
    });
    await test.step(`Verify the updated DATE OF BIRTH reflects in this page`, async () => {
      await performAction('validateFullPartyDetails', {
        defName: party,
        defDOB: date,
        defAddress: partyAddress,
        question: removeParty,
        option: checkFullPartyDetails.yesRadioOption,
        dobVal: true
      });
    });
  });

  test(`Case management - As a Case Worker Update Claimant details on the case  @CM @regression`, async () => {
    let date = CaseManagementCommonUtils.getRandomDate(updatePartyDetails.dateTypeHiddenUserInput);
    let submitPayLoad = submitCaseApiData.submitCasePayloadCaseFileView as Record<string, any>;
    await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
    await performValidation('mainHeader', manageParty.mainHeader);
    await performAction('selectParty', {
      question1: manageParty.whatChangeQuestion,
      option1: manageParty.updatePartyRadioOption,
      question2: manageParty.whichPartyContactInformationHiddenQuestion,
      option2: submitPayLoad.claimantName,
      nextPage: updatePartyDetails.mainHeader
    });
    await performAction('updatePartyDetails', {
      enterUKPostcodeTextLabel: updatePartyDetails.enterUKPostcodeTextLabel,
      postcode: updatePartyDetails.englandPostCodeTextInput,
      button: updatePartyDetails.findAddressButton,
      addressSelectLabel: updatePartyDetails.addressSelectHiddenLabel,
      addressIndex: updatePartyDetails.claimantAddressIndex,
      nextPage: checkYourAnswersManageParties.mainHeader
    });
    await performAction('clickButton', checkYourAnswersManageParties.submitButton);
    await performAction('confirmPartyDetailsUpdated', {
      userType: `Claimant's details`,
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage parties');
  });

  test(`Case management - As a Case Worker Successfully Remove a 'Defendant' from the case - @CM @regression`, async () => {
    const [party, party1, party2] = allPartyDetails;
    const [name, name1, name2] = [party, party1, party2].map(
      p => p.split(' - ')[0]
    );

    const partyAddress = getDefendantAddressByName(submitPayload, createPayload, name1);

    const removeParty =
      `${checkFullPartyDetails.doYouWantToRemoveHiddenQuestion} ${party1}?`;

    await test.step(`Select event "Manage Parties" from event drop down`, async () => {
      await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
      await performValidation('mainHeader', manageParty.mainHeader);
    });

    await test.step(`Select the party to be removed`, async () => {
      await performAction('selectParty', {
        question1: manageParty.whatChangeQuestion,
        option1: manageParty.removePartyRadioOption,
        question2: manageParty.whichPartyYouRemovingHiddenQuestion,
        option2: party1,
        nextPage: checkFullPartyDetails.mainHeader
      });
    });

    await test.step(`Full Party Details Page Validation`, async () => {
      await performAction('validateFullPartyDetails', {
        defName: party1,
        defDOB: 'Date of birth unknown',
        defAddress: partyAddress,
        question: removeParty,
        option: checkFullPartyDetails.yesRadioOption,
        nextPage: checkYourAnswersManageParties.mainHeader
      });
    });

    await performAction('clickButton', checkYourAnswersManageParties.submitButton);

    await performAction('confirmRemoveParty', {
      userType: 'Defendant',
      name: name1,
      submitPayload,
    });

    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage parties');

    await test.step(`Verify that the removed Defendant is not present on Case Tab - Case Parties Section`, async () => {
        await performValidation('mainHeader', home.caseParties);
        const defendantsToValidate = [
          { name: name, table: 'Defendant' },
          { name: name2, table: 'Additional defendant 1' },
        ];

        for (const defendant of defendantsToValidate) {
          const [firstName, lastName] = defendant.name.split(' ');
          await performAction('validateDefendantDetails', {
            firstName,
            lastName,
            mainTable: defendant.table,
            subTable: 'Service address',
            defAddress: getDefendantAddress(submitPayload, createPayload, defendant.name),
          });
        }
      }
    );   
  });

  test(`Case management - As a Case Worker select 'No' to remove party and attempt to Proceed @CM`, async () => {
    let party = allPartyDetails[0];
    let name = party.split(' - ')[0];
    let partyAddress = getDefendantAddressByName(submitPayload, createPayload, name);
    let removeParty = `${checkFullPartyDetails.doYouWantToRemoveHiddenQuestion} ${party}?`;

    await test.step(`Select event "Manage Parties" from event drop down`, async () => {
      await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
      await performValidation('mainHeader', manageParty.mainHeader);
    });
    await test.step(`Select the party to be removed`, async () => {
      await performAction('selectParty', {
        question1: manageParty.whatChangeQuestion,
        option1: manageParty.removePartyRadioOption,
        question2: manageParty.whichPartyYouRemovingHiddenQuestion,
        option2: party,
        nextPage: checkFullPartyDetails.mainHeader
      });
    });
    await test.step(`Full Party Details Page Validation when user selects No to remove party`, async () => {
      await performAction('validateFullPartyDetails', {
        defName: party,
        defDOB: 'Date of birth unknown',
        defAddress: partyAddress,
        question: removeParty,
        option: checkFullPartyDetails.noRadioOption,
        hiddenMessage: checkFullPartyDetails.noRemoveHiddenParagraph,
        errorHeader: checkFullPartyDetails.errorMessageHiddenHeader,
        errorMessage: checkFullPartyDetails.errorHiddenMessage,
      });
    });
  });

  test(`Case management - As a Case Worker attempt to Remove party from the case  'When there is an active GENAPP or Counter Claim ' @CM`, async () => {
    let party = allPartyDetails[0];
    await test.step(`Select event "Manage Parties" from event drop down`, async () => {
      await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
      await performValidation('mainHeader', manageParty.mainHeader);
    });
    await test.step(`Select the party to be removed when there is active Gen App`, async () => {
      await performAction('selectParty', {
        question1: manageParty.whatChangeQuestion,
        option1: manageParty.removePartyRadioOption,
        question2: manageParty.whichPartyYouRemovingHiddenQuestion,
        option2: party,
        activeApp: true,
        errorHeader: manageParty.errorMessageHiddenHeader,
        errorMessage: manageParty.errorAppHiddenMessage,
      });
    });
  });

  test(`Case management - As a Case worker attempt to Remove party from the case when there is 'Single Defendant' on the claim @CM`, async () => {
    await test.step(`Select event "Manage Parties" from event drop down`, async () => {
      await performAction('selectAnEvent', { eventType: caseSummary.manageParties });
      await performValidation('mainHeader', manageParty.mainHeader);
    });
    await test.step(`Validate that user cannot remove a claimant or defendant if only one of these parties exist on the case.`, async () => {
      await performAction('selectParty', {
        question1: manageParty.whatChangeQuestion,
        option1: manageParty.removePartyRadioOption,
        singleDefendant: true,
        errorHeader: manageParty.errorMessageHiddenHeader,
        errorMessage: manageParty.errorHiddenMessage,
      });
    });

  });
});
