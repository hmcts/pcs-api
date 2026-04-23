import {
  initializeExecutor,
  performAction,
  performValidation,
  performValidations
} from '@utils/controller';
import {
  addressCheckYourAnswers,
  addressDetails,
  checkYourAnswers,
  propertyDetails,
  reasonsForPossession,
  whatAreYourGroundsForPossession,
  home
} from '@data/page-data';
import {
  claimantType,
  claimType,
  claimantInformation,
  defendantDetails,
  contactPreferences,
  tenancyLicenceDetails,
  groundsForPossession,
  introductoryDemotedOrOtherGroundsForPossession,
  groundsForPossessionRentArrears,
  preactionProtocol,
  alternativesToPossession,
  claimantCircumstances,
  claimingCosts,
  dailyRentAmount,
  defendantCircumstances,
  mediationAndSettlement,
  moneyJudgment,
  noticeDetails,
  rentDetails,
  checkingNotice,
  additionalReasonsForPossession,
  generalApplication,
  completingYourClaim,
  rentArrears,
  claimLanguageUsed,
  underlesseeMortgageeEntitledToClaimRelief,
  wantToUploadDocuments,
  statementOfTruth,
  uploadAdditionalDocuments,
  demotionOfTenancyHousingActOptions,
  suspensionOfRightToBuyHousingActOptions,
  suspensionToBuyDemotionOfTenancyActs,
  demotionOfTenancyOrderReason,
  rentArrearsOrBreachOfTenancyGround,
  statementOfExpressTerms,
  suspensionOfRightToBuyOrderReason,
  suspensionToBuyDemotionOfTenancyOrderReasons,
  underlesseeMortgageeDetails
} from '@data/page-data-figma';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { test } from '@utils/test-fixtures';

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

test.describe('[Create Case - England] @nightly', async () => {
  test('England - Assured tenancy with Rent arrears and other possession grounds @PR', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption,
      phoneNumber: contactPreferences.noRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.postcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.yesRadioOption, numberOfDefendants: 2,
      name1Option: defendantDetails.yesRadioOption,
      correspondenceAddress1Option: defendantDetails.yesRadioOption, correspondenceAddressSame1Option: defendantDetails.yesRadioOption,
      name2Option: defendantDetails.noRadioOption,
      correspondenceAddress2Option: defendantDetails.yesRadioOption, correspondenceAddressSame2Option: defendantDetails.yesRadioOption});
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancyRadioOption,
      day: tenancyLicenceDetails.dayTextInput,
      month: tenancyLicenceDetails.monthTextInput,
      year: tenancyLicenceDetails.yearTextInput,
    });
    await performAction('selectGroundsForPossession',{groundsRadioInput: groundsForPossession.yesRadioOption});
    await performAction('selectRentArrearsPossessionGround', {
       rentArrears: [groundsForPossessionRentArrears.rentArrearsGround10Checkbox, groundsForPossessionRentArrears.seriousRentArrearsGroundCheckbox, groundsForPossessionRentArrears.persistentDelayInPayingRentCheckbox],
       otherGrounds: groundsForPossessionRentArrears.yesRadioOption
     });
    await performValidation('elementNotToBeVisible',[groundsForPossessionRentArrears.rentArrearsGround10Checkbox, groundsForPossessionRentArrears.seriousRentArrearsGroundCheckbox, groundsForPossessionRentArrears.persistentDelayInPayingRentCheckbox]);
    await performAction('clickLinkAndVerifyNewTabTitle', whatAreYourGroundsForPossession.moreInfoLink,groundsForPossession.mainHeader);
    await performAction('selectYourPossessionGrounds',{
       mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet,whatAreYourGroundsForPossession.mandatory.ownerOccupier],
       discretionary: [whatAreYourGroundsForPossession.discretionary.domesticViolence14A,whatAreYourGroundsForPossession.discretionary.suitableAlternativeAccommodation],
     });
    await performAction('enterReasonForPossession',
       [whatAreYourGroundsForPossession.mandatory.holidayLet,whatAreYourGroundsForPossession.mandatory.ownerOccupier,
         whatAreYourGroundsForPossession.discretionary.domesticViolence14A,whatAreYourGroundsForPossession.discretionary.suitableAlternativeAccommodation])
    await performValidation('text', {"text": preactionProtocol.englandRegisteredProvidersDynamicParagraph, "elementType": "paragraph"});
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
       attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
       settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
     });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('clickLinkAndVerifyNewTabTitle', checkingNotice.guidanceOnPossessionLink, checkingNotice.mainHeaderEnglandNewTab);
    await performAction('selectNoticeOfYourIntention', {
       question: checkingNotice.haveYouServedNoticeToQuestion,
       option: checkingNotice.yesRadioOption
     });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
       howDidYouServeNotice: noticeDetails.byFirstClassPostOrRadioOption,
       day: '16', month: '07', year: '1985'});
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption:'Weekly', rentAmount:'800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
       calculateRentAmount: '£114.29',
       unpaidRentInteractiveOption: dailyRentAmount.noRadioOption,
       unpaidRentAmountPerDay: '20'
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
       circumstanceOption: claimantCircumstances.yesRadioOption,
       claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
     });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
       defendantCircumstance: defendantCircumstances.yesRadioOption,
       additionalDefendants: true
     });
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession');
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
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

  //This test must be run only in the Nightly jobs as it contains an exhaustive test sceanrio for 'Upload additional documents' page
  test('England - Assured tenancy with Rent arrears and no other mandatory or discretionary possession grounds - Nightly only (Contains exhaustive test scenario)', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption,
      phoneNumber: contactPreferences.noRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.postcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancyRadioOption
    });
    await performAction('selectGroundsForPossession',{groundsRadioInput: groundsForPossession.yesRadioOption});
    await performAction('selectRentArrearsPossessionGround', {
      rentArrears: [groundsForPossessionRentArrears.rentArrearsGround10Checkbox],
      otherGrounds: groundsForPossessionRentArrears.yesRadioOption
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossession.additionalGroundsForPossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet]
    });
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet]);
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.yesRadioOption
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byFirstClassPostOrRadioOption,
      day: '16', month: '07', year: '1985', files: 'NoticeDetails.pdf'});
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption:'Weekly', rentAmount:'800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.noRadioOption,
      unpaidRentAmountPerDay: '20'
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
      circumstanceOption: claimantCircumstances.yesRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.yesRadioOption,
      additionalDefendants: false
    });
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession');
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yesRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.yesRadioOption
    });
    await performAction('uploadAdditionalDocs', {
      documents: [
        {type: uploadAdditionalDocuments.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: uploadAdditionalDocuments.witnessStatementDropDownInput},
        {type: uploadAdditionalDocuments.rentStatementDropDownInput, fileName: 'rentStatement.pdf', description: uploadAdditionalDocuments.rentStatementDropDownInput},
        {type: uploadAdditionalDocuments.tenancyAgreementDropDownInput, fileName: 'tenancy.pdf', description: uploadAdditionalDocuments.tenancyAgreementDropDownInput},
        {type: uploadAdditionalDocuments.certificateOfServiceDropDownInput, fileName: 'certificateOfService.pdf', description: uploadAdditionalDocuments.certificateOfServiceDropDownInput},
        {type: uploadAdditionalDocuments.correspondenceFromClaimantDropDownInput, fileName: 'correspondenceFromClaimant.pdf', description: uploadAdditionalDocuments.correspondenceFromClaimantDropDownInput},
        {type: uploadAdditionalDocuments.correspondenceFromDefendantDropDownInput, fileName: 'correspondenceFromDefendant.pdf', description: uploadAdditionalDocuments.correspondenceFromDefendantDropDownInput},
        {type: uploadAdditionalDocuments.possessionNoticeDropDownInput, fileName: 'possessionNotice.pdf', description: uploadAdditionalDocuments.possessionNoticeDropDownInput},
        {type: uploadAdditionalDocuments.noticeForServiceDropDownInput, fileName: 'noticeForService.pdf', description: uploadAdditionalDocuments.noticeForServiceDropDownInput},
        {type: uploadAdditionalDocuments.photographicEvidenceDropDownInput, fileName: 'photographicEvidence.pdf', description: uploadAdditionalDocuments.photographicEvidenceDropDownInput},
        {type: uploadAdditionalDocuments.inspectionOrReportDropDownInput, fileName: 'inspectionOrReport.pdf', description: uploadAdditionalDocuments.inspectionOrReportDropDownInput},
        {type: uploadAdditionalDocuments.certificateOfSuitabilityDropDownInput, fileName: 'certificateOfSuitability.pdf', description: uploadAdditionalDocuments.certificateOfSuitabilityDropDownInput},
        {type: uploadAdditionalDocuments.legalAidCertificateDropDownInput, fileName: 'legalAidCertificate.pdf', description: uploadAdditionalDocuments.legalAidCertificateDropDownInput},
        {type: uploadAdditionalDocuments.otherDocumentDropDownInput, fileName: 'otherDocument.pdf', description: uploadAdditionalDocuments.otherDocumentDropDownInput},
      ]
    });
    await performAction('selectApplications', generalApplication.yesRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.submitAndPayForClaimRadioOption);
    await performAction('selectStatementOfTruth', {
      completedBy: statementOfTruth.claimantLegalRepresentativeRadioOption,
      signThisStatementCheckbox: statementOfTruth.signThisStatementHiddenCheckbox,
      fullNameTextInput: statementOfTruth.fullNameHiddenTextInput,
      nameOfFirmTextInput: statementOfTruth.nameOfFirmHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruth.positionOrOfficeHeldHiddenTextInput
    });
    await performAction('clickButton', checkYourAnswers.submitClaim);
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
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.postcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancyRadioOption,
      files: ['tenancyLicence.docx']
    });
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.noRadioOption});
    await performValidation('mainHeader', whatAreYourGroundsForPossession.groundsForPossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      mandatory : [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier],
      discretionary :[whatAreYourGroundsForPossession.discretionary.domesticViolence14A, whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture15]
    });
    await performValidation('mainHeader', reasonsForPossession.mainHeader);
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet,whatAreYourGroundsForPossession.mandatory.ownerOccupier,
        whatAreYourGroundsForPossession.discretionary.domesticViolence14A,whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture15]);
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
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
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.inTheAlternativeToPossessionQuestion
      , option: [alternativesToPossession.suspensionOfRightToBuyCheckBox]});
    await performValidation('mainHeader', suspensionOfRightToBuyHousingActOptions.mainHeader);
    await performAction('selectHousingAct', [{question: suspensionOfRightToBuyHousingActOptions.whichSectionSuspensionOfRightToBuyQuestion
      , option: suspensionOfRightToBuyHousingActOptions.section6A1988RadioOption}]);
    await performValidation('mainHeader', suspensionOfRightToBuyOrderReason.mainHeader);
    await performAction('enterReasonForSuspensionOrder', suspensionOfRightToBuyOrderReason.whyAreYouRequestingSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
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
    await performAction('selectApplications', generalApplication.noRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.welshRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('England - Other tenancy with grounds for possession - Demoted tenancy', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption,
      phoneNumber: contactPreferences.noRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.postcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.introductoryTenancyRadioOption});
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yesRadioOption,rentArrears: groundsForPossession.noRadioOption,
      grounds: [introductoryDemotedOrOtherGroundsForPossession.rentArrearsHiddenCheckbox, introductoryDemotedOrOtherGroundsForPossession.antisocialBehaviourHiddenCheckbox,
        introductoryDemotedOrOtherGroundsForPossession.breachOfTheTenancyHiddenCheckbox, introductoryDemotedOrOtherGroundsForPossession.absoluteGroundsHiddenCheckbox,introductoryDemotedOrOtherGroundsForPossession.otherHiddenCheckbox]});
    await performAction('enterReasonForPossession'
      , [introductoryDemotedOrOtherGroundsForPossession.antisocialBehaviourHiddenCheckbox, introductoryDemotedOrOtherGroundsForPossession.breachOfTheTenancyHiddenCheckbox, introductoryDemotedOrOtherGroundsForPossession.absoluteGroundsHiddenCheckbox,introductoryDemotedOrOtherGroundsForPossession.otherHiddenCheckbox]);
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.yesRadioOption
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byPersonallyHandingItToRadioOption,
      explanationLabel: noticeDetails.nameOfPersonTheDocumentWasLeftHiddenTextLabel,
      explanation: noticeDetails.nameOfPersonTheDocumentWasLeftHiddenTextInput,
      day: '31', month: '01', year: '1962', hour: '10', minute: '55', second: '30'});
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption:'Weekly', rentAmount:'800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.noRadioOption,
      unpaidRentAmountPerDay: '20'
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
      circumstanceOption: claimantCircumstances.yesRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.inTheAlternativeToPossessionQuestion
      , option: [alternativesToPossession.demotionOfTenancyCheckBox]});
    await performValidation('mainHeader', demotionOfTenancyHousingActOptions.mainHeader);
    await performAction('selectHousingAct', [{question: demotionOfTenancyHousingActOptions.whichSectionDemotionOfTenancyQuestion
      , option: demotionOfTenancyHousingActOptions.section82A1985RadioOption}]);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.yesRadioOption);
    await performValidation('mainHeader', demotionOfTenancyOrderReason.mainHeader);
    await performAction('enterReasonForDemotionOrder', demotionOfTenancyOrderReason.whyAreYouRequestingDemotionOrderQuestion);
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
      addressOption: underlesseeMortgageeDetails.noRadioOption,
      anotherUnderlesseeOrMortgageeOption: underlesseeMortgageeDetails.noRadioOption
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.noRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishAndWelshRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
    await performAction('claimSaved')
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('England - Introductory tenancy with grounds for possession - excludes rent arrears', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption,
      phoneNumber: contactPreferences.noRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.postcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.introductoryTenancyRadioOption });
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yesRadioOption,rentArrears: groundsForPossession.noRadioOption,
      grounds: [introductoryDemotedOrOtherGroundsForPossession.antisocialBehaviourHiddenCheckbox, introductoryDemotedOrOtherGroundsForPossession.breachOfTheTenancyHiddenCheckbox]});
    await performAction('enterReasonForPossession'
      , [introductoryDemotedOrOtherGroundsForPossession.antisocialBehaviourHiddenCheckbox, introductoryDemotedOrOtherGroundsForPossession.breachOfTheTenancyHiddenCheckbox]);
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.yesRadioOption
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byPersonallyHandingItToRadioOption,
      explanationLabel: noticeDetails.nameOfPersonTheDocumentWasLeftHiddenTextLabel,
      explanation: noticeDetails.nameOfPersonTheDocumentWasLeftHiddenTextInput,
      day: '31', month: '01', year: '1962', hour: '10', minute: '55', second: '30'});
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.inTheAlternativeToPossessionQuestion
      , option: [alternativesToPossession.demotionOfTenancyCheckBox]});
    await performValidation('mainHeader', demotionOfTenancyHousingActOptions.mainHeader);
    await performAction('selectHousingAct', [{question: demotionOfTenancyHousingActOptions.whichSectionDemotionOfTenancyQuestion
      , option: demotionOfTenancyHousingActOptions.section82A1985RadioOption}]);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.yesRadioOption);
    await performValidation('mainHeader', demotionOfTenancyOrderReason.mainHeader);
    await performAction('enterReasonForDemotionOrder', demotionOfTenancyOrderReason.whyAreYouRequestingDemotionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.noRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishAndWelshRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
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
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption,
      phoneNumber: contactPreferences.noRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.postcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.demotedTenancyRadioOption});
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.noRadioOption,rentArrears: groundsForPossession.noRadioOption});
    await performAction('enterReasonForPossession', [groundsForPossession.noRadioOption]);
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.yesRadioOption
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byEmailRadioOption,
      explanationLabel: noticeDetails.explainHowItWasServedByEmailHiddenTextLabel,
      explanation: noticeDetails.explainHowItWasServedByEmailHiddenTextInput,
      day: '29', month: '02', year: '2000', hour: '16', minute: '01', second: '56'});
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.inTheAlternativeToPossessionQuestion
      , option: [alternativesToPossession.suspensionOfRightToBuyCheckBox, alternativesToPossession.demotionOfTenancyCheckBox]});
    await performValidation('mainHeader', suspensionToBuyDemotionOfTenancyActs.mainHeader);
    await performAction('selectHousingAct', [{question: suspensionToBuyDemotionOfTenancyActs.whichSectionSuspensionOfRightToBuyQuestion
      , option: suspensionToBuyDemotionOfTenancyActs.section6A1988RadioOption}
      , {question: suspensionToBuyDemotionOfTenancyActs.whichSectionDemotionOfTenancyQuestion
      , option: suspensionToBuyDemotionOfTenancyActs.section82A1985RadioOption}]);
    await performValidation('mainHeader', statementOfExpressTerms.mainHeader);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.yesRadioOption);
    await performValidation('mainHeader', suspensionToBuyDemotionOfTenancyOrderReasons.mainHeader);
    await performAction('enterReasonForSuspensionAndDemotionOrder'
      , {suspension: suspensionToBuyDemotionOfTenancyOrderReasons.whyAreYouRequestingSuspensionOrderQuestion
      ,  demotion: suspensionToBuyDemotionOfTenancyOrderReasons.whyAreYouRequestingDemotionOrderQuestion});
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeMortgageeDetails', {
      nameOption: underlesseeMortgageeDetails.noRadioOption,
      addressOption: underlesseeMortgageeDetails.noRadioOption,
      anotherUnderlesseeOrMortgageeOption: underlesseeMortgageeDetails.noRadioOption
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsQuestion,
      option: wantToUploadDocuments.noRadioOption
    });
    await performAction('selectApplications', generalApplication.noRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });

  test('England - Assured tenancy with Rent arrears and no other possession grounds - Demoted tenancy', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
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
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancyRadioOption});
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yesRadioOption});
    await performAction('selectRentArrearsPossessionGround', {
      rentArrears: [groundsForPossessionRentArrears.rentArrearsGround10Checkbox],
      otherGrounds: groundsForPossessionRentArrears.noRadioOption
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
    await performAction('provideRentDetails', {rentAmount:'850', rentFrequencyOption:'Other', inputFrequency:rentDetails.enterFrequencyHiddenTextInput,unpaidRentAmountPerDay:'50'});
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
      circumstanceOption: claimantCircumstances.yesRadioOption,
      claimantInput: claimantCircumstances.giveDetailsAboutCircumstancesIsRequiredTextInput
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {
      defendantCircumstance: defendantCircumstances.noRadioOption,
      additionalDefendants: false
    });
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.inTheAlternativeToPossessionQuestion
      , option: [alternativesToPossession.demotionOfTenancyCheckBox]});
    await performValidation('mainHeader', demotionOfTenancyHousingActOptions.mainHeader);
    await performAction('selectHousingAct', [{question: demotionOfTenancyHousingActOptions.whichSectionDemotionOfTenancyQuestion
      , option: demotionOfTenancyHousingActOptions.section6A1988RadioOption}]);
    await performValidation('mainHeader', statementOfExpressTerms.mainHeader);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.noRadioOption);
    await performValidation('mainHeader', demotionOfTenancyOrderReason.mainHeader);
    await performAction('enterReasonForDemotionOrder', demotionOfTenancyOrderReason.whyAreYouRequestingDemotionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeMortgageeDetails', {
      nameOption: underlesseeMortgageeDetails.noRadioOption,
      addressOption: underlesseeMortgageeDetails.yesRadioOption, address: underlesseeMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeMortgageeDetails.noRadioOption
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
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption,
      phoneNumber: contactPreferences.noRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.noRadioOption,
      correspondenceAddressOption: defendantDetails.noRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.flexibleTenancyRadioOption});
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossession.discretionary.rentArrearsOrBreachOfTenancy]
    });
    await performValidation('mainHeader', rentArrearsOrBreachOfTenancyGround.mainHeader);
    await performAction('selectRentArrearsOrBreachOfTenancy', {
      rentArrearsOrBreach: [rentArrearsOrBreachOfTenancyGround.rentArrearsCheckbox]
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.inTheAlternativeToPossessionQuestion
      , option: [alternativesToPossession.suspensionOfRightToBuyCheckBox]});
    await performValidation('mainHeader', suspensionToBuyDemotionOfTenancyActs.mainHeader);
    await performAction('selectHousingAct', [{question: suspensionToBuyDemotionOfTenancyActs.whichSectionSuspensionOfRightToBuyQuestion
      , option: suspensionToBuyDemotionOfTenancyActs.section82A1985RadioOption}]);
    await performValidation('mainHeader', suspensionOfRightToBuyOrderReason.mainHeader);
    await performAction('enterReasonForSuspensionOrder', suspensionOfRightToBuyOrderReason.whyAreYouRequestingSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yesRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeMortgageeDetails', {
      nameOption: underlesseeMortgageeDetails.noRadioOption,
      addressOption: underlesseeMortgageeDetails.yesRadioOption, address: underlesseeMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeMortgageeDetails.noRadioOption
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

  test('England - Secure tenancy with Rent and other grounds @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.noRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
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
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.secureTenancyRadioOption
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossession.groundsForPossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossession.discretionary.rentArrearsOrBreachOfTenancy, whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture4],
      mandatory: [whatAreYourGroundsForPossession.mandatory.antiSocialBehaviour],
      mandatoryAccommodation: [whatAreYourGroundsForPossession.mandatoryWithAccommodation.charitableLandlords, whatAreYourGroundsForPossession.mandatoryWithAccommodation.landlordsWorks],
      discretionaryAccommodation: [whatAreYourGroundsForPossession.discretionaryWithAccommodation.adapted, whatAreYourGroundsForPossession.discretionaryWithAccommodation.tied],
    });
    await performValidation('mainHeader', rentArrearsOrBreachOfTenancyGround.mainHeader);
    await performAction('selectRentArrearsOrBreachOfTenancy', {
      rentArrearsOrBreach: [rentArrearsOrBreachOfTenancyGround.breachOfTheTenancyCheckbox, rentArrearsOrBreachOfTenancyGround.rentArrearsCheckbox]
    });
    await performValidation('mainHeader', reasonsForPossession.mainHeader);
    await performAction('enterReasonForPossession'
      , [whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture4, whatAreYourGroundsForPossession.mandatory.antiSocialBehaviour,
        whatAreYourGroundsForPossession.mandatoryWithAccommodation.charitableLandlords, whatAreYourGroundsForPossession.mandatoryWithAccommodation.landlordsWorks,
        whatAreYourGroundsForPossession.discretionaryWithAccommodation.adapted, whatAreYourGroundsForPossession.discretionaryWithAccommodation.tied,
        reasonsForPossession.breachOfTenancy
      ]);
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.yesRadioOption
    });
    await performAction('selectNoticeDetails', {
       howDidYouServeNotice: noticeDetails.byOtherElectronicMethodRadioOption,
      day: '25', month: '02', year: '1970', hour: '22', minute: '45', second: '10'});
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.inTheAlternativeToPossessionQuestion
      , option: [alternativesToPossession.suspensionOfRightToBuyCheckBox, alternativesToPossession.demotionOfTenancyCheckBox]});
    await performValidation('mainHeader', suspensionToBuyDemotionOfTenancyActs.mainHeader);
    await performAction('selectHousingAct', [{question: suspensionToBuyDemotionOfTenancyActs.whichSectionSuspensionOfRightToBuyQuestion
      , option: suspensionToBuyDemotionOfTenancyActs.section6A1988RadioOption}
      , {question: suspensionToBuyDemotionOfTenancyActs.whichSectionDemotionOfTenancyQuestion
        , option: suspensionToBuyDemotionOfTenancyActs.section82A1985RadioOption}]);
    await performValidation('mainHeader', statementOfExpressTerms.mainHeader);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.noRadioOption);
    await performValidation('mainHeader', suspensionToBuyDemotionOfTenancyOrderReasons.mainHeader);
    await performAction('enterReasonForSuspensionAndDemotionOrder'
      , {suspension: suspensionToBuyDemotionOfTenancyOrderReasons.whyAreYouRequestingSuspensionOrderQuestion
        ,  demotion: suspensionToBuyDemotionOfTenancyOrderReasons.whyAreYouRequestingDemotionOrderQuestion});
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noRadioOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeMortgageeDetails', {
      nameOption: underlesseeMortgageeDetails.noRadioOption,
      addressOption: underlesseeMortgageeDetails.noRadioOption,
      anotherUnderlesseeOrMortgageeOption: underlesseeMortgageeDetails.noRadioOption
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
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption,
      phoneNumber: contactPreferences.noRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yesRadioOption, firstName: defendantDetails.defendantsFirstNameTextInput, lastName: defendantDetails.defendantsLastNameTextInput,
      correspondenceAddressOption: defendantDetails.yesRadioOption, correspondenceAddressSameOption: defendantDetails.noRadioOption, address: defendantDetails.postcodeTextInput,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancyRadioOption });
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.noRadioOption});
    await performValidation('mainHeader', whatAreYourGroundsForPossession.groundsForPossessionMainHeader);
    await performAction('clickLinkAndVerifyNewTabTitle', whatAreYourGroundsForPossession.moreInfoLink, groundsForPossession.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      mandatory : [whatAreYourGroundsForPossession.mandatory.seriousRentArrears],
      discretionary :[whatAreYourGroundsForPossession.discretionary.persistentDelayInPayingRent]
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
      option: checkingNotice.yesRadioOption
    });
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byEmailRadioOption,
      explanationLabel: noticeDetails.explainHowItWasServedByEmailHiddenTextLabel,
      explanation: noticeDetails.explainHowItWasServedByEmailHiddenTextInput,
      day: '29', month: '02', year: '2000', hour: '16', minute: '01', second: '56'
    });
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption: 'Weekly', rentAmount: '800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.noRadioOption,
      unpaidRentAmountPerDay: '20'
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.inTheAlternativeToPossessionQuestion
      , option: [alternativesToPossession.suspensionOfRightToBuyCheckBox]});
    await performValidation('mainHeader', suspensionToBuyDemotionOfTenancyActs.mainHeader);
    await performAction('selectHousingAct', [{question: suspensionToBuyDemotionOfTenancyActs.whichSectionSuspensionOfRightToBuyQuestion
      , option: suspensionToBuyDemotionOfTenancyActs.section6A1988RadioOption}]);
    await performValidation('mainHeader', suspensionOfRightToBuyOrderReason.mainHeader);
    await performAction('enterReasonForSuspensionOrder', suspensionOfRightToBuyOrderReason.whyAreYouRequestingSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
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
    await performAction('selectApplications', generalApplication.noRadioOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.welshRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveClaim);
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
    await performAction('selectClaimantType', claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performAction('selectClaimantName', claimantInformation.yesRadioOption);
    await performAction('clickButtonAndVerifyPageNavigation', claimantInformation.continueButton, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yesRadioOption,
      correspondenceAddress: contactPreferences.yesRadioOption,
      phoneNumber: contactPreferences.noRadioOption
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.noRadioOption,
      correspondenceAddressOption: defendantDetails.noRadioOption,
      addAdditionalDefendantsOption: defendantDetails.noRadioOption
    });
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.flexibleTenancyRadioOption});
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossession.discretionary.rentArrearsOrBreachOfTenancy]
    });
    await performValidation('mainHeader', rentArrearsOrBreachOfTenancyGround.mainHeader);
    await performAction('selectRentArrearsOrBreachOfTenancy', {
      rentArrearsOrBreach: [rentArrearsOrBreachOfTenancyGround.breachOfTheTenancyCheckbox]
    });
    await performAction('enterReasonForPossession', [reasonsForPossession.breachOfTenancy]);
    await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
      settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: checkingNotice.haveYouServedNoticeToQuestion,
      option: checkingNotice.yesRadioOption,
    });
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byEmailRadioOption,
      explanationLabel: noticeDetails.explainHowItWasServedByEmailHiddenTextLabel,
      explanation: noticeDetails.explainHowItWasServedByEmailHiddenTextInput});
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
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession', {question: alternativesToPossession.inTheAlternativeToPossessionQuestion
      , option: [alternativesToPossession.suspensionOfRightToBuyCheckBox]});
    await performValidation('mainHeader', suspensionToBuyDemotionOfTenancyActs.mainHeader);
    await performAction('selectHousingAct', [{question: suspensionToBuyDemotionOfTenancyActs.whichSectionSuspensionOfRightToBuyQuestion
      , option: suspensionToBuyDemotionOfTenancyActs.section121A1985RadioOption}]);
    await performValidation('mainHeader', suspensionOfRightToBuyOrderReason.mainHeader);
    await performAction('enterReasonForSuspensionOrder', suspensionOfRightToBuyOrderReason.whyAreYouRequestingSuspensionOrderQuestion);
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
});
