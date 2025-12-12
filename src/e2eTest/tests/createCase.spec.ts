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
  additionalReasonsForPossession,
  alternativesToPossession,
  applications,
  checkYourAnswers,
  claimantCircumstances,
  claimantName,
  claimantType,
  claimingCosts,
  claimType,
  completeYourClaim,
  contactPreferences,
  dailyRentAmount,
  defendantCircumstances,
  defendantDetails,
  detailsOfRentArrears,
  groundsForPossession,
  home,
  housingAct,
  languageUsed,
  mediationAndSettlement,
  moneyJudgment,
  noticeDetails,
  noticeOfYourIntention,
  preActionProtocol,
  propertyDetails,
  reasonsForPossession,
  reasonsForRequestingADemotionOrder,
  reasonsForRequestingASuspensionAndDemotionOrder,
  reasonsForRequestingASuspensionOrder,
  rentArrearsOrBreachOfTenancy,
  rentArrearsPossessionGrounds,
  rentDetails,
  signInOrCreateAnAccount,
  statementOfExpressTerms,
  statementOfTruth,
  tenancyLicenceDetails,
  underlesseeOrMortgageeDetails,
  underlesseeOrMortgageeEntitledToClaim,
  uploadAdditionalDocs,
  user,
  wantToUploadDocuments,
  whatAreYourGroundsForPossession
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

test.describe('[Create Case - England]', async () => {
  test('England - Assured tenancy with Rent arrears and other possession grounds @PR @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.defendantPostcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.yesRadioOption, numberOfDefendants: 2,
      name1Option: defendantDetails.yesRadioOption,
      correspondenceAddress1Option: defendantDetails.yesRadioOption, correspondenceAddressSame1Option: defendantDetails.yesRadioOption,
      name2Option: defendantDetails.noRadioOption,
      correspondenceAddress2Option: defendantDetails.yesRadioOption, correspondenceAddressSame2Option: defendantDetails.yesRadioOption});
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy,
      day: tenancyLicenceDetails.day,
      month: tenancyLicenceDetails.month,
      year: tenancyLicenceDetails.year,
      files: ['tenancyLicence.docx']
    });
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession',{groundsRadioInput: groundsForPossession.yes});
    await performAction('selectRentArrearsPossessionGround', {
      rentArrears: [rentArrearsPossessionGrounds.rentArrears, rentArrearsPossessionGrounds.seriousRentArrears, rentArrearsPossessionGrounds.persistentDelayInPayingRent],
      otherGrounds: rentArrearsPossessionGrounds.yes
    });
    await performValidation('elementNotToBeVisible',[rentArrearsPossessionGrounds.rentArrears, rentArrearsPossessionGrounds.seriousRentArrears, rentArrearsPossessionGrounds.persistentDelayInPayingRent]);
    await performAction('clickLinkAndVerifyNewTabTitle', whatAreYourGroundsForPossession.moreInfoLink,groundsForPossession.mainHeader);
    await performAction('selectYourPossessionGrounds',{
      mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet,whatAreYourGroundsForPossession.mandatory.ownerOccupier],
      discretionary: [whatAreYourGroundsForPossession.discretionary.domesticViolence14A,whatAreYourGroundsForPossession.discretionary.suitableAlternativeAccommodation],
    });
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet,whatAreYourGroundsForPossession.mandatory.ownerOccupier,
        whatAreYourGroundsForPossession.discretionary.domesticViolence14A,whatAreYourGroundsForPossession.discretionary.suitableAlternativeAccommodation])
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', {"text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText"});
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byFirstClassPost,
      day: '16', month: '07', year: '1985', files: 'NoticeDetails.pdf'});
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption:'weekly', rentAmount:'800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.no,
      unpaidRentAmountPerDay: '20'
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
      circumstanceOption: claimantCircumstances.yes,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: true
    });
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession');
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
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

  test('England - Assured tenancy with Rent arrears and no other mandatory or discretionary possession grounds', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.defendantPostcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy
    });
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession',{groundsRadioInput: groundsForPossession.yes});
    await performAction('selectRentArrearsPossessionGround', {
      rentArrears: [rentArrearsPossessionGrounds.rentArrears],
      otherGrounds: rentArrearsPossessionGrounds.yes
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossession.additionalGroundsForPossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet]
    });
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet]);
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', {"text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText"});
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byFirstClassPost,
      day: '16', month: '07', year: '1985'});
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption:'weekly', rentAmount:'800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.no,
      unpaidRentAmountPerDay: '20'
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
      circumstanceOption: claimantCircumstances.yes,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: false
    });
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession');
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yes);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.no});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.yes
    });
    await performAction('uploadAdditionalDocs', {
      documents: [{
        type: uploadAdditionalDocs.tenancyAgreementOption,
        fileName: 'tenancy.pdf',
        description: uploadAdditionalDocs.shortDescriptionInput
      }]
    });
    await performAction('selectApplications', applications.yes);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.submitAndClaimNow);
    await performAction('selectStatementOfTruth', {
      completedBy: statementOfTruth.claimantLegalRepresentativeRadioOption,
      signThisStatementCheckbox: statementOfTruth.signThisStatementHiddenCheckbox,
      fullNameTextInput: statementOfTruth.fullNameHiddenTextInput,
      nameOfFirmTextInput: statementOfTruth.nameOfFirmHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruth.positionOrOfficeHeldHiddenTextInput
    });
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('payClaimFee',{clickLink: true});
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performValidations(
      'address info not null',
      ['formLabelValue', propertyDetails.buildingAndStreetLabel],
      ['formLabelValue', propertyDetails.townOrCityLabel],
      ['formLabelValue', propertyDetails.postcodeZipcodeLabel],
      ['formLabelValue', propertyDetails.countryLabel],
    )
  });

  // The sections commented out will be fixed as part of the User Story https://tools.hmcts.net/jira/browse/HDPI-2123
  test('England - Assured tenancy with No Rent arrears', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.defendantPostcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy
    });
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.no});
    await performValidation('mainHeader', whatAreYourGroundsForPossession.groundsForPossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      mandatory : [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier],
      discretionary :[whatAreYourGroundsForPossession.discretionary.domesticViolence14A, whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture15]
    });
    await performValidation('mainHeader', reasonsForPossession.mainHeader);
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet,whatAreYourGroundsForPossession.mandatory.ownerOccupier,
        whatAreYourGroundsForPossession.discretionary.domesticViolence14A,whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture15]);
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', {"text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText"});
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.no
    });
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.suspensionOrDemotion
      , option: [alternativesToPossession.suspensionOfRightToBuy]});
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section6AHousingAct1988}]);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performAction('enterReasonForSuspensionOrder', reasonsForRequestingASuspensionOrder.requestSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.no);
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
    await performAction('selectApplications', applications.no);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.welsh});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('England - Other tenancy with grounds for possession - Demoted tenancy @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.defendantPostcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.other});
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yes,
      grounds: [groundsForPossession.rentArrears, groundsForPossession.antiSocialBehaviour,
        groundsForPossession.breachOfTheTenancy, groundsForPossession.absoluteGrounds,groundsForPossession.other]});
    await performAction('enterReasonForPossession'
      , [groundsForPossession.antiSocialBehaviour, groundsForPossession.breachOfTheTenancy, groundsForPossession.absoluteGrounds,groundsForPossession.otherGrounds]);
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', {"text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText"});
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byPersonallyHandling,
      explanationLabel: noticeDetails.nameOfPersonDocumentWasLeftLabel,
      explanation: noticeDetails.byPersonallyHandlingExplanationInput,
      day: '31', month: '01', year: '1962', hour: '10', minute: '55', second: '30'});
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption:'weekly', rentAmount:'800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.no,
      unpaidRentAmountPerDay: '20'
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
      circumstanceOption: claimantCircumstances.yes,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.suspensionOrDemotion
      , option: [alternativesToPossession.demotionOfTenancy]});
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.demotionOfTenancy.whichSection
      , option: housingAct.demotionOfTenancy.section82AHousingAct1985}]);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.yes);
    await performValidation('mainHeader', reasonsForRequestingADemotionOrder.mainHeader);
    await performAction('enterReasonForDemotionOrder', reasonsForRequestingADemotionOrder.requestDemotionOrderQuestion);
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
      addressOption: underlesseeOrMortgageeDetails.noRadioOption,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.no);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.englishAndWelsh});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('claimSaved')
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('England - Introductory tenancy with grounds for possession - excludes rent arrears @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.defendantPostcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.introductoryTenancy });
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yes,
      grounds: [groundsForPossession.antiSocialBehaviour, groundsForPossession.breachOfTheTenancy]});
    await performAction('enterReasonForPossession'
      , [groundsForPossession.antiSocialBehaviour, groundsForPossession.breachOfTheTenancy]);
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', {"text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText"});
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byPersonallyHandling,
      explanationLabel: noticeDetails.nameOfPersonDocumentWasLeftLabel,
      explanation: noticeDetails.byPersonallyHandlingExplanationInput,
      day: '31', month: '01', year: '1962', hour: '10', minute: '55', second: '30'});
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.suspensionOrDemotion
      , option: [alternativesToPossession.demotionOfTenancy]});
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.demotionOfTenancy.whichSection
      , option: housingAct.demotionOfTenancy.section82AHousingAct1985}]);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.yes);
    await performValidation('mainHeader', reasonsForRequestingADemotionOrder.mainHeader);
    await performAction('enterReasonForDemotionOrder', reasonsForRequestingADemotionOrder.requestDemotionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.no});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.no);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.englishAndWelsh});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('England - Demoted tenancy with no grounds for possession', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.defendantPostcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.demotedTenancy});
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.no});
    await performAction('enterReasonForPossession', [groundsForPossession.noGrounds]);
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', {"text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText"});
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byEmail,
      explanationLabel: noticeDetails.explainHowServedByEmailLabel,
      explanation: noticeDetails.byEmailExplanationInput,
      day: '29', month: '02', year: '2000', hour: '16', minute: '01', second: '56'});
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.suspensionOrDemotion
      , option: [alternativesToPossession.suspensionOfRightToBuy, alternativesToPossession.demotionOfTenancy]});
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section6AHousingAct1988}
      , {question: housingAct.demotionOfTenancy.whichSection
      , option: housingAct.demotionOfTenancy.section82AHousingAct1985}]);
    await performValidation('mainHeader', statementOfExpressTerms.mainHeader);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.yes);
    await performValidation('mainHeader', reasonsForRequestingASuspensionAndDemotionOrder.mainHeader);
    await performAction('enterReasonForSuspensionAndDemotionOrder'
      , {suspension: reasonsForRequestingASuspensionAndDemotionOrder.requestSuspensionOrderQuestion
      ,  demotion: reasonsForRequestingASuspensionAndDemotionOrder.requestDemotionOrderQuestion});
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.yes});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.noRadioOption,
      addressOption: underlesseeOrMortgageeDetails.noRadioOption,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', applications.no);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('England - Assured tenancy with Rent arrears and no other possession grounds - Demoted tenancy @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
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
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy});
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yes});
    await performAction('selectRentArrearsPossessionGround', {
      rentArrears: [rentArrearsPossessionGrounds.rentArrears],
      otherGrounds: rentArrearsPossessionGrounds.no
    });
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
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentAmount:'850', rentFrequencyOption:'Other', inputFrequency:rentDetails.rentFrequencyFortnightly,unpaidRentAmountPerDay:'50'});
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.demotionOfTenancy
      , option: [alternativesToPossession.demotionOfTenancy]});
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.demotionOfTenancy.whichSection
      , option: housingAct.demotionOfTenancy.section6AHousingAct1988}]);
    await performValidation('mainHeader', statementOfExpressTerms.mainHeader);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.no);
    await performValidation('mainHeader', reasonsForRequestingADemotionOrder.mainHeader);
    await performAction('enterReasonForDemotionOrder', reasonsForRequestingADemotionOrder.requestDemotionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.no);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option: underlesseeOrMortgageeEntitledToClaim.yes});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.noRadioOption,
      addressOption: underlesseeOrMortgageeDetails.yesRadioOption, address: underlesseeOrMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
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

  test('England - Flexible tenancy with Rent arrears only', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.noRadioOption,
      correspondenceAddressOption: defendantDetails.noRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.flexibleTenancy});
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossession.discretionary.rentArrearsOrBreachOfTenancy]
    });
    await performValidation('mainHeader', rentArrearsOrBreachOfTenancy.mainHeader);
    await performAction('selectRentArrearsOrBreachOfTenancy', {
      rentArrearsOrBreach: [rentArrearsOrBreachOfTenancy.rentArrears]
    });
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.suspensionOrDemotion
      , option: [alternativesToPossession.suspensionOfRightToBuy]});
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section121AHousingAct1985}]);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performAction('enterReasonForSuspensionOrder', reasonsForRequestingASuspensionOrder.requestSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.no);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yes);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option:underlesseeOrMortgageeEntitledToClaim.yes});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.noRadioOption,
      addressOption: underlesseeOrMortgageeDetails.yesRadioOption, address: underlesseeOrMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
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

  test('England - Secure tenancy with Rent and other grounds', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.no);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
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
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.secureTenancy
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossession.groundsForPossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossession.discretionary.rentArrearsOrBreachOfTenancy, whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture4],
      mandatory: [whatAreYourGroundsForPossession.mandatory.antiSocialBehaviour],
      mandatoryAccommodation: [whatAreYourGroundsForPossession.mandatoryWithAccommodation.charitableLandlords, whatAreYourGroundsForPossession.mandatoryWithAccommodation.landlordsWorks],
      discretionaryAccommodation: [whatAreYourGroundsForPossession.discretionaryWithAccommodation.adapted, whatAreYourGroundsForPossession.discretionaryWithAccommodation.tied],
    });
    await performValidation('mainHeader', rentArrearsOrBreachOfTenancy.mainHeader);
    await performAction('selectRentArrearsOrBreachOfTenancy', {
      rentArrearsOrBreach: [rentArrearsOrBreachOfTenancy.breachOfTenancy, rentArrearsOrBreachOfTenancy.rentArrears]
    });
    await performValidation('mainHeader', reasonsForPossession.mainHeader);
    await performAction('enterReasonForPossession'
      , [whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture4, whatAreYourGroundsForPossession.mandatory.antiSocialBehaviour,
        whatAreYourGroundsForPossession.mandatoryWithAccommodation.charitableLandlords, whatAreYourGroundsForPossession.mandatoryWithAccommodation.landlordsWorks,
        whatAreYourGroundsForPossession.discretionaryWithAccommodation.adapted, whatAreYourGroundsForPossession.discretionaryWithAccommodation.tied,
        reasonsForPossession.breachOfTenancy
      ]);
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
      option: noticeOfYourIntention.yes
    });
    await performAction('selectNoticeDetails', {
       howDidYouServeNotice: noticeDetails.byOtherElectronicMethod,
      day: '25', month: '02', year: '1970', hour: '22', minute: '45', second: '10'});
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.suspensionOrDemotion
      , option: [alternativesToPossession.suspensionOfRightToBuy, alternativesToPossession.demotionOfTenancy]});
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section6AHousingAct1988}
      , {question: housingAct.demotionOfTenancy.whichSection
        , option: housingAct.demotionOfTenancy.section82AHousingAct1985}]);
    await performValidation('mainHeader', statementOfExpressTerms.mainHeader);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.no);
    await performValidation('mainHeader', reasonsForRequestingASuspensionAndDemotionOrder.mainHeader);
    await performAction('enterReasonForSuspensionAndDemotionOrder'
      , {suspension: reasonsForRequestingASuspensionAndDemotionOrder.requestSuspensionOrderQuestion
        ,  demotion: reasonsForRequestingASuspensionAndDemotionOrder.requestDemotionOrderQuestion});
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
      option:underlesseeOrMortgageeEntitledToClaim.yes});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.noRadioOption,
      addressOption: underlesseeOrMortgageeDetails.noRadioOption,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
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

  test('England - Assured tenancy with ans no to rent arrears question, selects 08/10/11 grounds- routing flow @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.firstNameTextInput, lastName: defendantDetails.lastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.defendantPostcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy });
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.no});
    await performValidation('mainHeader', whatAreYourGroundsForPossession.groundsForPossessionMainHeader);
    await performAction('clickLinkAndVerifyNewTabTitle', whatAreYourGroundsForPossession.moreInfoLink, groundsForPossession.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      mandatory : [whatAreYourGroundsForPossession.mandatory.seriousRentArrears],
      discretionary :[whatAreYourGroundsForPossession.discretionary.persistentDelayInPayingRent]
    });
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', {"text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText"});
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes
    });
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byEmail,
      explanationLabel: noticeDetails.explainHowServedByEmailLabel,
      explanation: noticeDetails.byEmailExplanationInput,
      day: '29', month: '02', year: '2000', hour: '16', minute: '01', second: '56'
    });
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption: 'weekly', rentAmount: '800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.no,
      unpaidRentAmountPerDay: '20'
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.suspensionOrDemotion
      , option: [alternativesToPossession.suspensionOfRightToBuy]});
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section6AHousingAct1988}]);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performAction('enterReasonForSuspensionOrder', reasonsForRequestingASuspensionOrder.requestSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
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
    await performAction('selectApplications', applications.no);
    await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.welsh});
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('England - Flexible tenancy with Breach only', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.noRadioOption,
      correspondenceAddressOption: defendantDetails.noRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.flexibleTenancy});
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossession.discretionary.rentArrearsOrBreachOfTenancy]
    });
    await performValidation('mainHeader', rentArrearsOrBreachOfTenancy.mainHeader);
    await performAction('selectRentArrearsOrBreachOfTenancy', {
      rentArrearsOrBreach: [rentArrearsOrBreachOfTenancy.breachOfTenancy]
    });
    await performAction('enterReasonForPossession', [reasonsForPossession.breachOfTenancy]);
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes,
    });
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byEmail,
      explanationLabel: noticeDetails.explainHowServedByEmailLabel,
      explanation: noticeDetails.byEmailExplanationInput});
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.suspensionOrDemotion
      , option: [alternativesToPossession.suspensionOfRightToBuy]});
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section121AHousingAct1985}]);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performAction('enterReasonForSuspensionOrder', reasonsForRequestingASuspensionOrder.requestSuspensionOrderQuestion);
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
});
