import {test} from '@playwright/test';
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
  home,
  housingAct,
  propertyDetails,
  reasonsForPossession,
  reasonsForRequestingADemotionOrder,
  reasonsForRequestingASuspensionAndDemotionOrder,
  reasonsForRequestingASuspensionOrder,
  rentArrearsOrBreachOfTenancy,
  signInOrCreateAnAccount,
  statementOfExpressTerms,
  underlesseeOrMortgageeDetails,
  user,
  whatAreYourGroundsForPossession,
} from '@data/page-data';
import{
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
  payClaimFee,
  underlesseeMortgageeEntitledToClaimRelief
} from '@data/page-data-figma';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import {wantToUploadDocuments} from "@data/page-data-figma/wantToUploadDocuments.page.data";
import {statementOfTruth} from "@data/page-data-figma/statementOfTruth.page.data";
import {uploadAdditionalDocuments} from "@data/page-data-figma/uploadAdditionalDocuments.page.data";

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
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
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
      files: ['tenancyLicence.docx']
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
     await performValidation('text', {"text": preactionProtocol.registeredProvidersDynamicParagraph, "elementType": "paragraph"});
     await performAction('selectPreActionProtocol', preactionProtocol.yesRadioOption);
     await performValidation('mainHeader', mediationAndSettlement.mainHeader);
     await performAction('selectMediationAndSettlement', {
       attemptedMediationWithDefendantsOption: mediationAndSettlement.yesRadioOption,
       settlementWithDefendantsOption: mediationAndSettlement.noRadioOption,
     });
     await performValidation('mainHeader', checkingNotice.mainHeader);
     await performValidation('text', {"text": checkingNotice.guidanceOnPossessionLink, "elementType": "paragraphLink"})
     await performValidation('text', {"text": checkingNotice.youMayHaveAlreadyServedEnglandDynamicParagraph, "elementType": "paragraph"});
     await performAction('clickLinkAndVerifyNewTabTitle', checkingNotice.guidanceOnPossessionLink, checkingNotice.mainHeaderEnglandNewTab);
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
      rentPaidByOthersOption: rentArrears.yesOption,
      paymentOptions: [rentArrears.universalCreditCheckBox, rentArrears.otherCheckBox]
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
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yesOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', generalApplication.yesOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.submitAndPayForClaimRadioOption);
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
    await performValidation('text', {"text": checkingNotice.guidanceOnPossessionLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": checkingNotice.haveYouServedNoticeToQuestion, "elementType": "inlineText"});
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
      rentPaidByOthersOption: rentArrears.yesOption,
      paymentOptions: [rentArrears.universalCreditCheckBox, rentArrears.otherCheckBox]
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
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yesOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.yes
    });
    await performAction('uploadAdditionalDocs', {
      documents: [{
        type: uploadAdditionalDocuments.tenancyAgreementOption,
        fileName: 'tenancy.pdf',
        description: uploadAdditionalDocuments.shortDescriptionInput
      }]
    });
    await performAction('selectApplications', generalApplication.yesOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.submitAndPayForClaimRadioOption);
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
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancyRadioOption
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
    await performValidation('text', {"text": checkingNotice.guidanceOnPossessionLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": checkingNotice.haveYouServedNoticeToQuestion, "elementType": "inlineText"});
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
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section6AHousingAct1988}]);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performAction('enterReasonForSuspensionOrder', reasonsForRequestingASuspensionOrder.requestSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
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
    await performAction('selectApplications', generalApplication.noOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.welshRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
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
    await performValidation('text', {"text": checkingNotice.guidanceOnPossessionLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": checkingNotice.haveYouServedNoticeToQuestion, "elementType": "inlineText"});
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
      rentPaidByOthersOption: rentArrears.yesOption,
      paymentOptions: [rentArrears.universalCreditCheckBox, rentArrears.otherCheckBox]
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
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.demotionOfTenancy.whichSection
      , option: housingAct.demotionOfTenancy.section82AHousingAct1985}]);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.yes);
    await performValidation('mainHeader', reasonsForRequestingADemotionOrder.mainHeader);
    await performAction('enterReasonForDemotionOrder', reasonsForRequestingADemotionOrder.requestDemotionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.yesRadioOption, name: underlesseeOrMortgageeDetails.underlesseeNameTextInput,
      addressOption: underlesseeOrMortgageeDetails.noRadioOption,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', generalApplication.noOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishAndWelshRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
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
    await performValidation('text', {"text": checkingNotice.guidanceOnPossessionLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": checkingNotice.haveYouServedNoticeToQuestion, "elementType": "inlineText"});
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
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.demotionOfTenancy.whichSection
      , option: housingAct.demotionOfTenancy.section82AHousingAct1985}]);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.yes);
    await performValidation('mainHeader', reasonsForRequestingADemotionOrder.mainHeader);
    await performAction('enterReasonForDemotionOrder', reasonsForRequestingADemotionOrder.requestDemotionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noOption);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', generalApplication.noOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishAndWelshRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
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
    await performValidation('text', {"text": checkingNotice.guidanceOnPossessionLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": checkingNotice.haveYouServedNoticeToQuestion, "elementType": "inlineText"});
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
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.noRadioOption,
      addressOption: underlesseeOrMortgageeDetails.noRadioOption,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', generalApplication.noOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
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
      files: ['rentArrears.docx', 'rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: rentArrears.yesOption,
      paymentOptions: [rentArrears.universalCreditCheckBox, rentArrears.otherCheckBox]
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
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.demotionOfTenancy.whichSection
      , option: housingAct.demotionOfTenancy.section6AHousingAct1988}]);
    await performValidation('mainHeader', statementOfExpressTerms.mainHeader);
    await performAction('selectStatementOfExpressTerms', statementOfExpressTerms.no);
    await performValidation('mainHeader', reasonsForRequestingADemotionOrder.mainHeader);
    await performAction('enterReasonForDemotionOrder', reasonsForRequestingADemotionOrder.requestDemotionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.noRadioOption,
      addressOption: underlesseeOrMortgageeDetails.yesRadioOption, address: underlesseeOrMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', generalApplication.yesOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
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
    await performValidation('mainHeader', rentArrearsOrBreachOfTenancy.mainHeader);
    await performAction('selectRentArrearsOrBreachOfTenancy', {
      rentArrearsOrBreach: [rentArrearsOrBreachOfTenancy.rentArrears]
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
      rentPaidByOthersOption: rentArrears.yesOption,
      paymentOptions: [rentArrears.universalCreditCheckBox, rentArrears.otherCheckBox]
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
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section121AHousingAct1985}]);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performAction('enterReasonForSuspensionOrder', reasonsForRequestingASuspensionOrder.requestSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yesOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.noRadioOption,
      addressOption: underlesseeOrMortgageeDetails.yesRadioOption, address: underlesseeOrMortgageeDetails.underlesseePostcodeTextInput,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', generalApplication.yesOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
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
      rentPaidByOthersOption: rentArrears.yesOption,
      paymentOptions: [rentArrears.universalCreditCheckBox, rentArrears.otherCheckBox]
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
    await performAction('selectClaimingCosts', claimingCosts.yesRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.yesRadioOption});
    await performAction('selectUnderlesseeOrMortgageeDetails', {
      nameOption: underlesseeOrMortgageeDetails.noRadioOption,
      addressOption: underlesseeOrMortgageeDetails.noRadioOption,
      anotherUnderlesseeOrMortgageeOption: underlesseeOrMortgageeDetails.noRadioOption
    });
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', generalApplication.yesOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
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
    await performValidation('text', {"text": checkingNotice.guidanceOnPossessionLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": checkingNotice.haveYouServedNoticeToQuestion, "elementType": "inlineText"});
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
      rentPaidByOthersOption: rentArrears.yesOption,
      paymentOptions: [rentArrears.universalCreditCheckBox, rentArrears.otherCheckBox]
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
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section6AHousingAct1988}]);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performAction('enterReasonForSuspensionOrder', reasonsForRequestingASuspensionOrder.requestSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.noOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', generalApplication.noOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.welshRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
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
    await performValidation('mainHeader', rentArrearsOrBreachOfTenancy.mainHeader);
    await performAction('selectRentArrearsOrBreachOfTenancy', {
      rentArrearsOrBreach: [rentArrearsOrBreachOfTenancy.breachOfTenancy]
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
    await performValidation('mainHeader', housingAct.mainHeader);
    await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
      , option: housingAct.suspensionOfRightToBuy.section121AHousingAct1985}]);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
    await performAction('enterReasonForSuspensionOrder', reasonsForRequestingASuspensionOrder.requestSuspensionOrderQuestion);
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.noRadioOption);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeaderCaption);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yesOption);
    await performValidation('mainHeader', underlesseeMortgageeEntitledToClaimRelief.mainHeader);
    await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
      question: underlesseeMortgageeEntitledToClaimRelief.isThereAnUnderlesseeQuestion,
      option: underlesseeMortgageeEntitledToClaimRelief.noRadioOption});
    await performAction('wantToUploadDocuments', {
      question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
      option: wantToUploadDocuments.no
    });
    await performAction('selectApplications', generalApplication.yesOption);
    await performAction('selectLanguageUsed', {question: claimLanguageUsed.whichLanguageDidYouUseQuestion, option: claimLanguageUsed.englishLRadioOption});
    await performAction('completingYourClaim', completingYourClaim.saveItForLaterRadioOption);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });
});
