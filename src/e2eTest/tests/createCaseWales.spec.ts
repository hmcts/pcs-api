import { test } from '@playwright/test';
import {
  initializeExecutor,
  performAction,
  performValidation,
  performValidations
} from '@utils/controller';
import {
  addressCheckYourAnswers,
  addressDetails,
  claimantCircumstances,
  claimantDetailsWales,
  claimantName,
  claimantType,
  claimingCosts,
  claimType,
  contactPreferences,
  dailyRentAmount,
  defendantCircumstances,
  defendantDetails,
  detailsOfRentArrears,
  home,
  mediationAndSettlement,
  moneyJudgment,
  noticeOfYourIntention,
  occupationContractOrLicenceDetailsWales,
  preActionProtocol,
  prohibitedConductStandardContractWales,
  rentDetails,
  reasonsForPossession,
  signInOrCreateAnAccount,
  asbQuestionsWales,
  noticeDetails,
  user,
  whatAreYourGroundsForPossessionWales,
  underlesseeOrMortgageeEntitledToClaim,
  additionalReasonsForPossession,
  wantToUploadDocuments,
  applications,
  languageUsed,
  completeYourClaim,
  checkYourAnswers,
  propertyDetails,
  underlesseeOrMortgageeDetails,
  statementOfTruth
} from '@data/page-data';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAdditionalCookiesButton,
    hide: signInOrCreateAnAccount.hideThisCookieMessageButton
  });
  await performAction('login', user.claimantSolicitor);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAnalyticsCookiesButton
  });
  await performAction('clickTab', home.createCaseTab);
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.afterEach(async () => {
  PageContentValidation.finaliseTest();
});

test.describe('[Create Case - Wales]', async () => {
  test('Wales - Secure contract - Rent arrears only @PR @regression', async () => {
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
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
        {question1: claimantDetailsWales.wereYouRegisteredUnderPart1OfTheHousingAct2014, option1: claimantDetailsWales.yes,
         question2: claimantDetailsWales.wereYouLicensedUnderPart1OfTheHousingAct2014, option2: claimantDetailsWales.yes,
         question3: claimantDetailsWales.haveYouAppointedALicenseAgent, option3: claimantDetailsWales.yes});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.no,
      correspondenceAddress: contactPreferences.no,
      phoneNumber: contactPreferences.yes
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType,
      occupationContractType: occupationContractOrLicenceDetailsWales.secureContract,
      day: occupationContractOrLicenceDetailsWales.dayInput,
      month: occupationContractOrLicenceDetailsWales.monthInput,
      year: occupationContractOrLicenceDetailsWales.yearInput,
      files: 'occupationContract.pdf'
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('clickLinkAndVerifyNewTabTitle', whatAreYourGroundsForPossessionWales.moreInfoLink, whatAreYourGroundsForPossessionWales.understandingThePossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.rentArrears]
    });
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveQuestion,
      option: noticeOfYourIntention.no
    });
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£32.85',
      unpaidRentInteractiveOption: dailyRentAmount.yes
    });
    await performValidation('mainHeader', detailsOfRentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: detailsOfRentArrears.yes,
      paymentOptions: [detailsOfRentArrears.universalCreditOption, detailsOfRentArrears.paymentOtherOption]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.no);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductStandardContractWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductStandardContractWales.no,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.no});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
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
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
      {question1: claimantDetailsWales.wereYouRegisteredUnderPart1OfTheHousingAct2014, option1: claimantDetailsWales.yes,
        question2: claimantDetailsWales.wereYouLicensedUnderPart1OfTheHousingAct2014, option2: claimantDetailsWales.yes,
        question3: claimantDetailsWales.haveYouAppointedALicenseAgent, option3: claimantDetailsWales.yes});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.no,
      correspondenceAddress: contactPreferences.no,
      phoneNumber: contactPreferences.yes
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
     await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType,
      occupationContractType: occupationContractOrLicenceDetailsWales.secureContract
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
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
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveQuestion,
      option: noticeOfYourIntention.no
    });
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {
      rentAmount: '850',
      rentFrequencyOption: 'Other',
      inputFrequency: rentDetails.rentFrequencyFortnightly,
      unpaidRentAmountPerDay: '50'
    });
    await performValidation('mainHeader', detailsOfRentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.docx', 'rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: detailsOfRentArrears.yes,
      paymentOptions: [detailsOfRentArrears.universalCreditOption, detailsOfRentArrears.paymentOtherOption]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.yes,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performValidation('mainHeader', prohibitedConductStandardContractWales.mainHeader);
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductStandardContractWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductStandardContractWales.yes,
      label1: prohibitedConductStandardContractWales.whyAreYouMakingThisClaimLabel,
      input1: prohibitedConductStandardContractWales.whyAreYouMakingThisClaimSampleData,
      question2: prohibitedConductStandardContractWales.haveYouAndContractHolderAgreedQuestion,
      option2: prohibitedConductStandardContractWales.yes,
      label2: prohibitedConductStandardContractWales.giveDetailsOfTermsLabel,
      input2: prohibitedConductStandardContractWales.giveDetailsOfTermsSampleData
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    // The following sections are commented out pending development of the Wales journey.
    await performAction('selectClaimingCosts', claimingCosts.no);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.no});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.english
    });
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
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
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
        {question1: claimantDetailsWales.wereYouRegisteredUnderPart1OfTheHousingAct2014, option1: claimantDetailsWales.no,
         question2: claimantDetailsWales.wereYouLicensedUnderPart1OfTheHousingAct2014, option2: claimantDetailsWales.no,
         question3: claimantDetailsWales.haveYouAppointedALicenseAgent, option3: claimantDetailsWales.no});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType,
      occupationContractType: occupationContractOrLicenceDetailsWales.standardContract,
      day: occupationContractOrLicenceDetailsWales.dayInput,
      month: occupationContractOrLicenceDetailsWales.monthInput,
      year: occupationContractOrLicenceDetailsWales.yearInput
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.rentArrears, whatAreYourGroundsForPossessionWales.discretionary.antiSocialBehaviour],
    });
    await performAction('selectAsb', {
      asbChoice: asbQuestionsWales.yesRadioOption, giveDetailsOfAsb: asbQuestionsWales.giveDetailsOfAsbHiddenTextInput,
      illegalPurposesChoice: asbQuestionsWales.yesRadioOption, giveDetailsOfIllegal: asbQuestionsWales.giveDetailsOfIllegalHiddenTextInput,
      prohibitedConductChoice: asbQuestionsWales.yesRadioOption, giveDetailsOfTheOther: asbQuestionsWales.giveDetailsOfTheOtherHiddenTextInput
    });
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveQuestion,
      option: noticeOfYourIntention.no,
    });
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performAction('selectDailyRentAmount', {
    calculateRentAmount: '£32.85',
    unpaidRentInteractiveOption: dailyRentAmount.yes
    });
    await performValidation('mainHeader', detailsOfRentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: detailsOfRentArrears.yes,
      paymentOptions: [detailsOfRentArrears.universalCreditOption, detailsOfRentArrears.paymentOtherOption]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductStandardContractWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductStandardContractWales.yes,
      label1: prohibitedConductStandardContractWales.whyAreYouMakingThisClaimLabel,
      input1: prohibitedConductStandardContractWales.whyAreYouMakingThisClaimSampleData,
      question2: prohibitedConductStandardContractWales.haveYouAndContractHolderAgreedQuestion,
      option2: prohibitedConductStandardContractWales.no,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.no);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yes);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.no});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
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
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.no);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
        {question1: claimantDetailsWales.wereYouRegisteredUnderPart1OfTheHousingAct2014, option1: claimantDetailsWales.notApplicable,
         question2: claimantDetailsWales.wereYouLicensedUnderPart1OfTheHousingAct2014, option2: claimantDetailsWales.notApplicable,
         question3: claimantDetailsWales.haveYouAppointedALicenseAgent, option3: claimantDetailsWales.notApplicable});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.no,
      correspondenceAddress: contactPreferences.no,
      phoneNumber: contactPreferences.yes
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType,
      occupationContractType: occupationContractOrLicenceDetailsWales.other
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
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveQuestion,
      option: noticeOfYourIntention.yes,
      typeOfNotice: noticeOfYourIntention.typeOfNoticeInput
    });
    await performAction('selectNoticeDetails', {
     howDidYouServeNotice: noticeDetails.byOtherElectronicMethod,
      day: '25', month: '02', year: '1970', hour: '22', minute: '45', second: '10', files: 'NoticeDetails.pdf'});
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductStandardContractWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductStandardContractWales.no,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.no});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('Wales - Secure contract - Rent arrears + other options @regression', async () => {
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
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
      {question1: claimantDetailsWales.wereYouRegisteredUnderPart1OfTheHousingAct2014, option1: claimantDetailsWales.yes,
        question2: claimantDetailsWales.wereYouLicensedUnderPart1OfTheHousingAct2014, option2: claimantDetailsWales.yes,
        question3: claimantDetailsWales.haveYouAppointedALicenseAgent, option3: claimantDetailsWales.yes});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.no,
      correspondenceAddress: contactPreferences.no,
      phoneNumber: contactPreferences.yes
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.defendantPostcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.yesRadioOption, numberOfDefendants: 2,
      name1Option: defendantDetails.yesRadioOption,
      correspondenceAddress1Option: defendantDetails.yesRadioOption, correspondenceAddressSame1Option: defendantDetails.yesRadioOption,
      name2Option: defendantDetails.noRadioOption,
      correspondenceAddress2Option: defendantDetails.yesRadioOption, correspondenceAddressSame2Option: defendantDetails.yesRadioOption});
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType,
      occupationContractType: occupationContractOrLicenceDetailsWales.secureContract,
      day: occupationContractOrLicenceDetailsWales.dayInput,
      month: occupationContractOrLicenceDetailsWales.monthInput,
      year: occupationContractOrLicenceDetailsWales.yearInput,
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
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveQuestion,
      option: noticeOfYourIntention.no
    });
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£32.85',
      unpaidRentInteractiveOption: dailyRentAmount.yes
    });
    await performValidation('mainHeader', detailsOfRentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: detailsOfRentArrears.yes,
      paymentOptions: [detailsOfRentArrears.universalCreditOption, detailsOfRentArrears.paymentOtherOption]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.no);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: true
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductStandardContractWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductStandardContractWales.no,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.yes});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.yesRadioOption, name: underlesseeOrMortgageeDetails.underlesseeNameTextInput,
      addressOption: underlesseeOrMortgageeDetails.yesRadioOption, address: underlesseeOrMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.yesRadioOption, additionalUnderlesseeMortgagees: 2,
      name1Option: underlesseeOrMortgageeDetails.yesRadioOption,
      correspondenceAddress1Option: underlesseeOrMortgageeDetails.noRadioOption,
      name2Option: underlesseeOrMortgageeDetails.noRadioOption,
      correspondenceAddress2Option: underlesseeOrMortgageeDetails.noRadioOption,
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.submitAndClaimNow);
    await performAction('selectStatementOfTruth', {
      completedBy: statementOfTruth.claimantRadioOption,
      iBelieveCheckbox: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
      fullNameTextInput: statementOfTruth.fullNameHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruth.positionOrOfficeHeldHiddenTextInput
    });
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
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
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
      {question1: claimantDetailsWales.wereYouRegisteredUnderPart1OfTheHousingAct2014, option1: claimantDetailsWales.yes,
        question2: claimantDetailsWales.wereYouLicensedUnderPart1OfTheHousingAct2014, option2: claimantDetailsWales.yes,
        question3: claimantDetailsWales.haveYouAppointedALicenseAgent, option3: claimantDetailsWales.yes});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.no,
      correspondenceAddress: contactPreferences.no,
      phoneNumber: contactPreferences.yes
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType,
      occupationContractType: occupationContractOrLicenceDetailsWales.standardContract
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.otherBreachOfContract]
    });
    await performAction('enterReasonForPossession',
        [whatAreYourGroundsForPossessionWales.discretionary.otherBreachOfContract]);
    // Following lines enabled to reach notice of your intention page as HDPI-2343 is done for Wales journey routing
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveQuestion,
      option: noticeOfYourIntention.no
    });
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductStandardContractWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductStandardContractWales.no,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.yes});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.yesRadioOption, name: underlesseeOrMortgageeDetails.underlesseeNameTextInput,
      addressOption: underlesseeOrMortgageeDetails.yesRadioOption, address: underlesseeOrMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.yesRadioOption, additionalUnderlesseeMortgagees: 2,
      name1Option: underlesseeOrMortgageeDetails.yesRadioOption,
      correspondenceAddress1Option: underlesseeOrMortgageeDetails.noRadioOption,
      name2Option: underlesseeOrMortgageeDetails.noRadioOption,
      correspondenceAddress2Option: underlesseeOrMortgageeDetails.noRadioOption,
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
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
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, claimantDetailsWales.mainHeader);
    await performAction('selectClaimantDetails',
      {question1: claimantDetailsWales.wereYouRegisteredUnderPart1OfTheHousingAct2014, option1: claimantDetailsWales.yes,
        question2: claimantDetailsWales.wereYouLicensedUnderPart1OfTheHousingAct2014, option2: claimantDetailsWales.yes,
        question3: claimantDetailsWales.haveYouAppointedALicenseAgent, option3: claimantDetailsWales.yes});
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.no,
      correspondenceAddress: contactPreferences.no,
      phoneNumber: contactPreferences.yes
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.yesRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType,
      occupationContractType: occupationContractOrLicenceDetailsWales.secureContract
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.rentArrears,whatAreYourGroundsForPossessionWales.discretionary.otherBreachOfContract]
    });
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossessionWales.discretionary.otherBreachOfContract]);
    // Following lines enabled to reach notice of your intention page as HDPI-2343 is done for Wales journey routing
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveQuestion,
      option: noticeOfYourIntention.no
    });
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£32.85',
      unpaidRentInteractiveOption: dailyRentAmount.yes
    });
    await performValidation('mainHeader', detailsOfRentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: detailsOfRentArrears.yes,
      paymentOptions: [detailsOfRentArrears.universalCreditOption, detailsOfRentArrears.paymentOtherOption]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.no);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: false
    });
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductStandardContractWales.areYouAlsoMakingAClaimQuestion,
      option1: prohibitedConductStandardContractWales.no,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.yes});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.yesRadioOption, name: underlesseeOrMortgageeDetails.underlesseeNameTextInput,
      addressOption: underlesseeOrMortgageeDetails.yesRadioOption, address: underlesseeOrMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.yesRadioOption, additionalUnderlesseeMortgagees: 2,
      name1Option: underlesseeOrMortgageeDetails.yesRadioOption,
      correspondenceAddress1Option: underlesseeOrMortgageeDetails.noRadioOption,
      name2Option: underlesseeOrMortgageeDetails.noRadioOption,
      correspondenceAddress2Option: underlesseeOrMortgageeDetails.noRadioOption,
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });
});
