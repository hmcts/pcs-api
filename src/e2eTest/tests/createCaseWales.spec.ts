import { test } from '@utils/test-fixtures';
import {
  initializeExecutor,
  performAction,
  performValidation,
  performValidations
} from '@utils/controller';
import {
  addressCheckYourAnswers,
  addressDetails,
  reasonsForPossession,
  asbQuestionsWales,
  whatAreYourGroundsForPossessionWales,
  checkYourAnswers,
  propertyDetails,
  home
} from '@data/page-data';
import {
  claimantType,
  claimType,
  claimantInformation,
  defendantDetails,
  contactPreferences,
  preactionProtocol,
  claimantCircumstances,
  claimingCosts,
  dailyRentAmount,
  defendantCircumstances,
  mediationAndSettlement,
  moneyJudgment,
  rentDetails,
  noticeDetails,
  checkingNotice,
  additionalReasonsForPossession,
  generalApplication,
  completingYourClaim,
  rentArrears,
  claimLanguageUsed,
  underlesseeMortgageeEntitledToClaimRelief,
  wantToUploadDocuments,
  statementOfTruth,
  claimantDetailsWales,
  occupationLicenceDetailsWales,
  prohibitedConductWales,
  underlesseeMortgageeDetails
} from '@data/page-data-figma';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('clickTab', home.createCaseTab);
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('[Create Case - Wales] @nightly', async () => {
  test('Wales - Standard Contract - Rent arrears only @PR @regression', async () => {
    await performAction('enterTestAddressManually', {
      buildingAndStreet: addressDetails.walesBuildingAndStreetTextInput,
      townOrCity: addressDetails.walesTownOrCityTextInput,
      county: addressDetails.walesCountyTextInput,
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      country: addressDetails.walesCountryTextInput
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.walesCommunityLandlordDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
        {question1: claimantDetailsWales.wereYouRegisteredUnderPart1Question, option1: claimantDetailsWales.yesRadioOption,
         question2: claimantDetailsWales.wereYouLicensedUnderPart1Question, option2: claimantDetailsWales.yesRadioOption,
         question3: claimantDetailsWales.haveYouAppointedALicensedAgentQuestion, option3: claimantDetailsWales.yesRadioOption});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.noRadioOption,
      correspondenceAddress: contactPreferences.noRadioOption,
      phoneNumber: contactPreferences.yesRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationLicenceDetailsWales.whatTypeOfOccupationContractQuestion,
      occupationContractType: occupationLicenceDetailsWales.standardContractRadioOption,
      day: occupationLicenceDetailsWales.dayTextInput,
      month: occupationLicenceDetailsWales.monthTextInput,
      year: occupationLicenceDetailsWales.yearTextInput,
      files: 'occupationContract.pdf'
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('clickLinkAndVerifyNewTabTitle', whatAreYourGroundsForPossessionWales.moreInfoLink, whatAreYourGroundsForPossessionWales.understandingThePossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.rentArrears, whatAreYourGroundsForPossessionWales.mandatory.section181, whatAreYourGroundsForPossessionWales.mandatory.section187],
    });
    await performValidation('text', {"text": preactionProtocol.walesCommunityLandlordsDynamicParagraph, "elementType": "paragraph"})
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('clickLinkAndVerifyNewTabTitle', checkingNotice.guidanceOnPossessionLink, checkingNotice.mainHeaderWalesNewTab);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.noRadioOption
    });
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£32.85',
      unpaidRentInteractiveOption: dailyRentAmount.yesRadioOption
    });
    await performValidation('mainHeader', rentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: rentArrears.yesRadioOption,
      paymentOptions: [rentArrears.universalCreditHiddenCheckBox, rentArrears.otherHiddenCheckBox]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.noRadioOption);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.noRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductWales.noRadioOption,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.yesRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('Wales - Secure contract - Rent arrears + ASB + other options @regression', async () => {
    await performAction('enterTestAddressManually', {
      buildingAndStreet: addressDetails.walesBuildingAndStreetTextInput,
      townOrCity: addressDetails.walesTownOrCityTextInput,
      county: addressDetails.walesCountyTextInput,
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      country: addressDetails.walesCountryTextInput
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.walesCommunityLandlordDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
      {question1: claimantDetailsWales.wereYouRegisteredUnderPart1Question, option1: claimantDetailsWales.yesRadioOption,
        question2: claimantDetailsWales.wereYouLicensedUnderPart1Question, option2: claimantDetailsWales.yesRadioOption,
        question3: claimantDetailsWales.haveYouAppointedALicensedAgentQuestion, option3: claimantDetailsWales.yesRadioOption});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.noRadioOption,
      correspondenceAddress: contactPreferences.noRadioOption,
      phoneNumber: contactPreferences.yesRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationLicenceDetailsWales.whatTypeOfOccupationContractQuestion,
      occupationContractType: occupationLicenceDetailsWales.secureContractRadioOption
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performValidation('elementNotToBeVisible',[whatAreYourGroundsForPossessionWales.mandatory.section181, whatAreYourGroundsForPossessionWales.mandatory.section187]);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.rentArrears, whatAreYourGroundsForPossessionWales.discretionary.antiSocialBehaviour, whatAreYourGroundsForPossessionWales.discretionary.estateManagementGrounds],
      discretionaryEstateGrounds: [whatAreYourGroundsForPossessionWales.discretionary.buildingWorks],
      mandatory: [whatAreYourGroundsForPossessionWales.mandatory.failureToGiveupPossession]
    });
    await performValidation('mainHeader', reasonsForPossession.mainHeader);
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossessionWales.mandatory.failureToGiveupPossession, whatAreYourGroundsForPossessionWales.discretionary.buildingWorks]);
    await performAction('selectAsb', {
      asbChoice: asbQuestionsWales.noRadioOption,
      illegalPurposesChoice: asbQuestionsWales.noRadioOption,
      prohibitedConductChoice: asbQuestionsWales.noRadioOption
    });
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.noRadioOption
    });
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {
      rentAmount: '850',
      rentFrequencyOption: 'Other',
      inputFrequency: rentDetails.enterFrequencyHiddenTextInput,
      unpaidRentAmountPerDay: '50'
    });
    await performValidation('mainHeader', rentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.docx', 'rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: rentArrears.yesRadioOption,
      paymentOptions: [rentArrears.universalCreditHiddenCheckBox, rentArrears.otherHiddenCheckBox]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.yesRadioOption);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.yesRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performValidation('mainHeader', prohibitedConductWales.mainHeader);
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductWales.yesRadioOption,
      label1: prohibitedConductWales.whyAreYouMakingThisClaimHiddenQuestion,
      input1: prohibitedConductWales.whyAreYouMakingThisClaimTextInput,
      question2: prohibitedConductWales.haveYouAndContractHolderAgreedHiddenQuestion,
      option2: prohibitedConductWales.yesRadioOption,
      label2: prohibitedConductWales.giveDetailsOfTermsHiddenTextLabel,
      input2: prohibitedConductWales.giveDetailsOfTermsTextInput
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    // The following sections are commented out pending development of the Wales journey.
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.yesRadioOption);
    await performAction('selectLanguageUsed', {
      question: claimLanguageUsed.whichLanguageDidYouUseQuestion,
      option: claimLanguageUsed.englishLRadioOption
    });
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('Wales - Standard contract - Rent arrears + ASB', async () => {
    await performAction('enterTestAddressManually', {
      buildingAndStreet: addressDetails.walesBuildingAndStreetTextInput,
      townOrCity: addressDetails.walesTownOrCityTextInput,
      county: addressDetails.walesCountyTextInput,
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      country: addressDetails.walesCountryTextInput
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.walesCommunityLandlordDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
        {question1: claimantDetailsWales.wereYouRegisteredUnderPart1Question, option1: claimantDetailsWales.noRadioOption,
         question2: claimantDetailsWales.wereYouLicensedUnderPart1Question, option2: claimantDetailsWales.noRadioOption,
         question3: claimantDetailsWales.haveYouAppointedALicensedAgentQuestion, option3: claimantDetailsWales.noRadioOption});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption,
      phoneNumber: contactPreferences.noRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationLicenceDetailsWales.whatTypeOfOccupationContractQuestion,
      occupationContractType: occupationLicenceDetailsWales.standardContractRadioOption,
      day: occupationLicenceDetailsWales.dayTextInput,
      month: occupationLicenceDetailsWales.monthTextInput,
      year: occupationLicenceDetailsWales.yearTextInput
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.rentArrears, whatAreYourGroundsForPossessionWales.discretionary.antiSocialBehaviour, whatAreYourGroundsForPossessionWales.mandatory.section181, whatAreYourGroundsForPossessionWales.mandatory.section187],
    });
    await performAction('selectAsb', {
      asbChoice: asbQuestionsWales.yesRadioOption, giveDetailsOfAsb: asbQuestionsWales.giveDetailsOfAsbHiddenTextInput,
      illegalPurposesChoice: asbQuestionsWales.yesRadioOption, giveDetailsOfIllegal: asbQuestionsWales.giveDetailsOfIllegalHiddenTextInput,
      prohibitedConductChoice: asbQuestionsWales.yesRadioOption, giveDetailsOfTheOther: asbQuestionsWales.giveDetailsOfTheOtherHiddenTextInput
    });
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.noRadioOption,
    });
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performAction('selectDailyRentAmount', {
    calculateRentAmount: '£32.85',
    unpaidRentInteractiveOption: dailyRentAmount.yesRadioOption
    });
    await performValidation('mainHeader', rentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: rentArrears.yesRadioOption,
      paymentOptions: [rentArrears.universalCreditHiddenCheckBox, rentArrears.otherHiddenCheckBox]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.yesRadioOption);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.noRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductWales.yesRadioOption,
      label1: prohibitedConductWales.whyAreYouMakingThisClaimHiddenQuestion,
      input1: prohibitedConductWales.whyAreYouMakingThisClaimTextInput,
      question2: prohibitedConductWales.haveYouAndContractHolderAgreedHiddenQuestion,
      option2: prohibitedConductWales.noRadioOption,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yesRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.yesRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('Wales - Other - No Rent arrears,  ASB + other options', async () => {
    await performAction('enterTestAddressManually', {
      buildingAndStreet: addressDetails.walesBuildingAndStreetTextInput,
      townOrCity: addressDetails.walesTownOrCityTextInput,
      county: addressDetails.walesCountyTextInput,
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      country: addressDetails.walesCountryTextInput
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.walesCommunityLandlordDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.noRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
        {question1: claimantDetailsWales.wereYouRegisteredUnderPart1Question, option1: claimantDetailsWales.notApplicableRadioOption,
         question2: claimantDetailsWales.wereYouLicensedUnderPart1Question, option2: claimantDetailsWales.notApplicableRadioOption,
         question3: claimantDetailsWales.haveYouAppointedALicensedAgentQuestion, option3: claimantDetailsWales.notApplicableRadioOption});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.noRadioOption,
      correspondenceAddress: contactPreferences.noRadioOption,
      phoneNumber: contactPreferences.yesRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationLicenceDetailsWales.whatTypeOfOccupationContractQuestion,
      occupationContractType: occupationLicenceDetailsWales.otherRadioOption
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.antiSocialBehaviour, whatAreYourGroundsForPossessionWales.discretionary.estateManagementGrounds],
      discretionaryEstateGrounds: [whatAreYourGroundsForPossessionWales.discretionary.buildingWorks],
      mandatory: [whatAreYourGroundsForPossessionWales.mandatory.section191],
    });
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossessionWales.discretionary.buildingWorks,whatAreYourGroundsForPossessionWales.mandatory.section191]);
    await performAction('clickButton', reasonsForPossession.continue);
    await performAction('selectAsb', {
      asbChoice: asbQuestionsWales.yesRadioOption,giveDetailsOfAsb: asbQuestionsWales.giveDetailsOfAsbHiddenTextInput,
      illegalPurposesChoice: asbQuestionsWales.noRadioOption,
      prohibitedConductChoice: asbQuestionsWales.yesRadioOption, giveDetailsOfTheOther: asbQuestionsWales.giveDetailsOfTheOtherHiddenTextInput
    });
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.yesRadioOption,
      typeOfNotice: 'What type of notice did you serve?',
      typeOfNoticeInput: 'RWH20'
    });
    await performAction('selectNoticeDetails', {
     howDidYouServeNotice: noticeDetails.byOtherElectronicMethodRadioOption,
      day: '25', month: '02', year: '1970', hour: '22', minute: '45', second: '10', files: 'NoticeDetails.pdf'});
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.noRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductWales.noRadioOption,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.yesRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('Wales - Secure contract - Rent arrears + estate grounds + other options @regression', async () => {
    await performAction('enterTestAddressManually', {
      buildingAndStreet: addressDetails.walesBuildingAndStreetTextInput,
      townOrCity: addressDetails.walesTownOrCityTextInput,
      county: addressDetails.walesCountyTextInput,
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      country: addressDetails.walesCountryTextInput
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.walesCommunityLandlordDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
      {question1: claimantDetailsWales.wereYouRegisteredUnderPart1Question, option1: claimantDetailsWales.yesRadioOption,
        question2: claimantDetailsWales.wereYouLicensedUnderPart1Question, option2: claimantDetailsWales.yesRadioOption,
        question3: claimantDetailsWales.haveYouAppointedALicensedAgentQuestion, option3: claimantDetailsWales.yesRadioOption});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.noRadioOption,
      correspondenceAddress: contactPreferences.noRadioOption,
      phoneNumber: contactPreferences.yesRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.postcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.yesRadioOption, numberOfDefendants: 2,
      name1Option: defendantDetails.yesRadioOption,
      correspondenceAddress1Option: defendantDetails.yesRadioOption, correspondenceAddressSame1Option: defendantDetails.yesRadioOption,
      name2Option: defendantDetails.noRadioOption,
      correspondenceAddress2Option: defendantDetails.yesRadioOption, correspondenceAddressSame2Option: defendantDetails.yesRadioOption});
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationLicenceDetailsWales.whatTypeOfOccupationContractQuestion,
      occupationContractType: occupationLicenceDetailsWales.secureContractRadioOption,
      day: occupationLicenceDetailsWales.dayTextInput,
      month: occupationLicenceDetailsWales.monthTextInput,
      year: occupationLicenceDetailsWales.yearTextInput,
      files: 'occupationContract.pdf'
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('clickLinkAndVerifyNewTabTitle', whatAreYourGroundsForPossessionWales.moreInfoLink, whatAreYourGroundsForPossessionWales.understandingThePossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.rentArrears, whatAreYourGroundsForPossessionWales.discretionary.estateManagementGrounds],
      discretionaryEstateGrounds: [whatAreYourGroundsForPossessionWales.discretionary.buildingWorks],
      mandatory: [whatAreYourGroundsForPossessionWales.mandatory.failureToGiveupPossession]
    });
    await performValidation('mainHeader', reasonsForPossession.mainHeader);
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossessionWales.mandatory.failureToGiveupPossession, whatAreYourGroundsForPossessionWales.discretionary.buildingWorks]);
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.noRadioOption
    });
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£32.85',
      unpaidRentInteractiveOption: dailyRentAmount.yesRadioOption
    });
    await performValidation('mainHeader', rentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: rentArrears.yesRadioOption,
      paymentOptions: [rentArrears.universalCreditHiddenCheckBox, rentArrears.otherHiddenCheckBox]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.noRadioOption);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.noRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: true
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductWales.noRadioOption,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeMortgageeDetails', {
      nameOption: underlesseeMortgageeDetails.yesRadioOption, name: underlesseeMortgageeDetails.underlesseeNameTextInput,
      addressOption: underlesseeMortgageeDetails.yesRadioOption, address: underlesseeMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeMortgageeDetails.yesRadioOption, additionalUnderlesseeMortgagees: 2,
      name1Option: underlesseeMortgageeDetails.yesRadioOption,
      correspondenceAddress1Option: underlesseeMortgageeDetails.noRadioOption,
      name2Option: underlesseeMortgageeDetails.noRadioOption,
      correspondenceAddress2Option: underlesseeMortgageeDetails.noRadioOption,
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.yesRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.submitAndPayForClaimRadioOption);
    await performAction('selectStatementOfTruth', {
      completedBy: statementOfTruth.claimantRadioOption,
      iBelieveCheckbox: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
      fullNameTextInput: statementOfTruth.fullNameHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruth.positionOrOfficeHeldHiddenTextInput
    });
    await performAction('clickButton', checkYourAnswers.submitClaim);
    await performAction('payClaimFee');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performValidations(
      'address info not null',
      ['formLabelValue', propertyDetails.buildingAndStreetLabel],
      ['formLabelValue', propertyDetails.townOrCityLabel],
      ['formLabelValue', propertyDetails.postcodeZipcodeLabel],
      ['formLabelValue', propertyDetails.countryLabel],
    )
  });

  test('Wales - Standard contract - No Rent arrears', async () => {
    await performAction('enterTestAddressManually', {
      buildingAndStreet: addressDetails.walesBuildingAndStreetTextInput,
      townOrCity: addressDetails.walesTownOrCityTextInput,
      county: addressDetails.walesCountyTextInput,
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      country: addressDetails.walesCountryTextInput
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.walesCommunityLandlordDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
      {question1: claimantDetailsWales.wereYouRegisteredUnderPart1Question, option1: claimantDetailsWales.yesRadioOption,
        question2: claimantDetailsWales.wereYouLicensedUnderPart1Question, option2: claimantDetailsWales.yesRadioOption,
        question3: claimantDetailsWales.haveYouAppointedALicensedAgentQuestion, option3: claimantDetailsWales.yesRadioOption});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.noRadioOption,
      correspondenceAddress: contactPreferences.noRadioOption,
      phoneNumber: contactPreferences.yesRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationLicenceDetailsWales.whatTypeOfOccupationContractQuestion,
      occupationContractType: occupationLicenceDetailsWales.standardContractRadioOption
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.otherBreachOfContract]
    });
    await performAction('enterReasonForPossession',
        [whatAreYourGroundsForPossessionWales.discretionary.otherBreachOfContract]);
    // Following lines enabled to reach notice of your intention page as HDPI-2343 is done for Wales journey routing
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.noRadioOption
    });
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.noRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductWales.noRadioOption,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeMortgageeDetails', {
      nameOption: underlesseeMortgageeDetails.yesRadioOption, name: underlesseeMortgageeDetails.underlesseeNameTextInput,
      addressOption: underlesseeMortgageeDetails.yesRadioOption, address: underlesseeMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeMortgageeDetails.yesRadioOption, additionalUnderlesseeMortgagees: 2,
      name1Option: underlesseeMortgageeDetails.yesRadioOption,
      correspondenceAddress1Option: underlesseeMortgageeDetails.noRadioOption,
      name2Option: underlesseeMortgageeDetails.noRadioOption,
      correspondenceAddress2Option: underlesseeMortgageeDetails.noRadioOption,
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.yesRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('Wales - Secure contract - Rent arrears + Other options @regression', async () => {
    await performAction('enterTestAddressManually', {
      buildingAndStreet: addressDetails.walesBuildingAndStreetTextInput,
      townOrCity: addressDetails.walesTownOrCityTextInput,
      county: addressDetails.walesCountyTextInput,
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      country: addressDetails.walesCountryTextInput
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.walesCommunityLandlordDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
      {question1: claimantDetailsWales.wereYouRegisteredUnderPart1Question, option1: claimantDetailsWales.yesRadioOption,
        question2: claimantDetailsWales.wereYouLicensedUnderPart1Question, option2: claimantDetailsWales.yesRadioOption,
        question3: claimantDetailsWales.haveYouAppointedALicensedAgentQuestion, option3: claimantDetailsWales.yesRadioOption});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.noRadioOption,
      correspondenceAddress: contactPreferences.noRadioOption,
      phoneNumber: contactPreferences.yesRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationLicenceDetailsWales.whatTypeOfOccupationContractQuestion,
      occupationContractType: occupationLicenceDetailsWales.secureContractRadioOption
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.rentArrears,whatAreYourGroundsForPossessionWales.discretionary.otherBreachOfContract]
    });
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossessionWales.discretionary.otherBreachOfContract]);
    // Following lines enabled to reach notice of your intention page as HDPI-2343 is done for Wales journey routing
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.noRadioOption
    });
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£32.85',
      unpaidRentInteractiveOption: dailyRentAmount.yesRadioOption
    });
    await performValidation('mainHeader', rentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: rentArrears.yesRadioOption,
      paymentOptions: [rentArrears.universalCreditHiddenCheckBox, rentArrears.otherHiddenCheckBox]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.noRadioOption);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.noRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductWales.noRadioOption,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeMortgageeDetails', {
      nameOption: underlesseeMortgageeDetails.yesRadioOption, name: underlesseeMortgageeDetails.underlesseeNameTextInput,
      addressOption: underlesseeMortgageeDetails.yesRadioOption, address: underlesseeMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeMortgageeDetails.yesRadioOption, additionalUnderlesseeMortgagees: 2,
      name1Option: underlesseeMortgageeDetails.yesRadioOption,
      correspondenceAddress1Option: underlesseeMortgageeDetails.noRadioOption,
      name2Option: underlesseeMortgageeDetails.noRadioOption,
      correspondenceAddress2Option: underlesseeMortgageeDetails.noRadioOption,
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.yesRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });
});
