import {test} from '@playwright/test';
import {initializeExecutor, performAction, performValidation, performValidations} from '@utils/controller';
import {claimType, claimantType, claimantName, claimantDetailsWales, contactPreferences, defendantDetails, tenancyLicenceDetails,
        groundsForPossession, rentArrearsPossessionGrounds, preActionProtocol, mediationAndSettlement, noticeOfYourIntention, rentDetails,
        provideMoreDetailsOfClaim, resumeClaim, resumeClaimOptions, detailsOfRentArrears, whatAreYourGroundsForPossession,
        rentArrearsOrBreachOfTenancy, reasonsForPossession, moneyJudgment, claimantCircumstances, applications, completeYourClaim, user,
        checkYourAnswers, propertyDetails, languageUsed, defendantCircumstances, claimingCosts, home, additionalReasonsForPossession,
        underlesseeOrMortgageeEntitledToClaim, wantToUploadDocuments, whatAreYourGroundsForPossessionWales, addressDetails,
        signInOrCreateAnAccount, occupationContractOrLicenceDetailsWales, prohibitedConductStandardContractWales, dailyRentAmount} from '@data/page-data';

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

test.describe('[Create Case - Wales] @Master @nightly', async () => {
  test('Wales - Secure contract', async () => {
    await performAction('enterTestAddressManually');
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
    await performAction('defendantDetails', {
      name: defendantDetails.yes,
      correspondenceAddress: defendantDetails.yes,
      email: defendantDetails.yes,
      correspondenceAddressSame: defendantDetails.yes
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
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.rentArrears],
      mandatory: [whatAreYourGroundsForPossessionWales.mandatory.failureToGiveupPossession]
    });
    await performAction('clickButton', reasonsForPossession.continue);
    // Following lines enabled to reach notice of your intention page as HDPI-2343 is done for Wales journey routing
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yes});
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.no
    });
    // Following lines enabled to reach the Prohibited conduct standard contract page as HDPI-2506
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
    await performAction('selectDefendantCircumstances', defendantCircumstances.no);
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
    /*await performAction('selectClaimingCosts', claimingCosts.no);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('clickButton', underlesseeOrMortgageeEntitledToClaim.continue);
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
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performValidations('address information entered',
      ['formLabelValue', propertyDetails.buildingAndStreetLabel, addressDetails.buildingAndStreet],
      ['formLabelValue', propertyDetails.addressLine2Label, addressDetails.addressLine2],
      ['formLabelValue', propertyDetails.townOrCityLabel, addressDetails.townOrCity],
      ['formLabelValue', propertyDetails.postcodeZipcodeLabel, addressDetails.walesCourtAssignedPostcode],
      ['formLabelValue', propertyDetails.countryLabel, addressDetails.country]);*/
  });

  test('Wales - Standard contract', async () => {
    await performAction('enterTestAddressManually');
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
    await performAction('defendantDetails', {
      name: defendantDetails.no,
      correspondenceAddress: defendantDetails.no,
      email: defendantDetails.no,
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
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.estateManagementGrounds,whatAreYourGroundsForPossessionWales.discretionary.rentArrears],
      discretionaryEstateGrounds: [whatAreYourGroundsForPossessionWales.discretionary.buildingWorks],
      mandatory: [whatAreYourGroundsForPossessionWales.mandatory.section187],
    });
    await performAction('clickButton', reasonsForPossession.continue);
    // Following lines enabled to reach notice of your intention page as HDPI-2343 is done for Wales journey routing
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yes});
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.no,
    });
    // Following lines enabled to reach the Prohibited conduct standard contract page as HDPI-2506
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performAction('selectDailyRentAmount', {
    calculateRentAmount: '£32.85',
    unpaidRentInteractiveOption: dailyRentAmount.yes
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', defendantCircumstances.no);
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductStandardContractWales.areYouAlsoMakingAClaimQuestion, option1: prohibitedConductStandardContractWales.yes,
      label1: prohibitedConductStandardContractWales.whyAreYouMakingThisClaimLabel, input1: prohibitedConductStandardContractWales.whyAreYouMakingThisClaimSampleData,
      question2: prohibitedConductStandardContractWales.haveYouAndContractHolderAgreedQuestion, option2: prohibitedConductStandardContractWales.no,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    // The following sections are commented out pending development of the Wales journey.
    /*await performAction('selectClaimingCosts', claimingCosts.no);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yes);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('clickButton', underlesseeOrMortgageeEntitledToClaim.continue);
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performValidations('address information entered',
      ['formLabelValue', propertyDetails.buildingAndStreetLabel, addressDetails.buildingAndStreet],
      ['formLabelValue', propertyDetails.addressLine2Label, addressDetails.addressLine2],
      ['formLabelValue', propertyDetails.townOrCityLabel, addressDetails.townOrCity],
      ['formLabelValue', propertyDetails.postcodeZipcodeLabel, addressDetails.walesCourtAssignedPostcode],
      ['formLabelValue', propertyDetails.countryLabel, addressDetails.country]);*/
  });

  test('Wales - Occupation contract Licence Details - Other - No Rent arrears', async () => {
    await performAction('enterTestAddressManually');
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
    await performAction('defendantDetails', {
      name: defendantDetails.yes,
      correspondenceAddress: defendantDetails.yes,
      email: defendantDetails.yes,
      correspondenceAddressSame: defendantDetails.yes
    });
    await performAction('selectOccupationContractOrLicenceDetails', {
      occupationContractQuestion: occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType,
      occupationContractType: occupationContractOrLicenceDetailsWales.other,
      files: 'occupationContract.pdf'
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.otherBreachOfContract],
    });
    await performAction('clickButton', reasonsForPossession.continue);
    // Following lines enabled to reach notice of your intention page as HDPI-2343 is done for Wales journey routing
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yes});
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes,
      typeOfNotice: noticeOfYourIntention.typeOfNoticeInput
    });
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', defendantCircumstances.yes);
    await performAction('selectProhibitedConductStandardContract', {
      question1: prohibitedConductStandardContractWales.areYouAlsoMakingAClaimQuestion, option1: prohibitedConductStandardContractWales.no,
    });
    await performValidation('mainHeader', claimingCosts.mainHeader);
    // The following sections are commented out pending development of the Wales journey.
    /*await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('clickButton', underlesseeOrMortgageeEntitledToClaim.continue);
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performValidations('address information entered',
      ['formLabelValue', propertyDetails.buildingAndStreetLabel, addressDetails.buildingAndStreet],
      ['formLabelValue', propertyDetails.addressLine2Label, addressDetails.addressLine2],
      ['formLabelValue', propertyDetails.townOrCityLabel, addressDetails.townOrCity],
      ['formLabelValue', propertyDetails.postcodeZipcodeLabel, addressDetails.walesCourtAssignedPostcode],
      ['formLabelValue', propertyDetails.countryLabel, addressDetails.country]);*/
  });
});
