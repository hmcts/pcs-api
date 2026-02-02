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
  applications,
  checkYourAnswers,
  completeYourClaim,
  detailsOfRentArrears,
  languageUsed,
  propertyDetails,
  reasonsForPossession,
  resumeClaim,
  resumeClaimOptions,
  statementOfTruth,
  underlesseeOrMortgageeEntitledToClaim,
  user,
  wantToUploadDocuments,
  whatAreYourGroundsForPossession,
  housingPossessionClaim,
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
  checkingNotice
} from '@data/page-data-figma';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { dismissCookieBanner } from '@config/cookie-banner';
import { startLogCapture, attachLogToTest } from '@utils/test-logger';

// This test validates the resume & find case functionality with and without saved options.
// It is not intended to reuse for any of the e2e scenarios, those should still be covered in others specs.
// When a new page is added/flow changes, basic conditions in this test should be updated accordingly to continue the journey.
// Due to frequent issues with relogin and "Find Case" (Elasticsearch), this test is made optional only for the pipeline to maintain a green build.
// However, it must be executed locally, and evidence of the passed results should be provided during PR review in case its failing in pipeline.

// Disable global storageState for this file - these tests need to test sign-out/re-login flow
test.use({ storageState: undefined });

test.beforeEach(async ({ page, context }, testInfo) => {
  await context.clearCookies();
  initializeExecutor(page);
  startLogCapture(page, testInfo);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await page.evaluate(() => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch (e) {
      // Ignore if storage is not accessible
    }
  });

  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.claimantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('clickTab', home.createCaseTab);
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.afterEach(async ({}, testInfo) => {
  await attachLogToTest(testInfo);
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('[Create Case - With resume claim options]', async () => {
  test('England - Resume with saved options - Assured Tenancy - Rent arrears + other grounds when user selects no to rent arrears question', async () => {
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
    await performAction('signOut');
    await performAction('reloginAndFindTheCase', user.claimantSolicitor);
    await performAction('clickButtonAndVerifyPageNavigation', resumeClaim.continue, resumeClaimOptions.mainHeader);
    await performAction('selectResumeClaimOption', resumeClaimOptions.yes);
    await performValidation('radioButtonChecked', claimantType.england.registeredProviderForSocialHousing, true);
    await performAction('verifyPageAndClickButton', claimantType.continueButton, claimantType.mainHeader);
    await performValidation('radioButtonChecked', claimType.noRadioOption, true);
    await performAction('verifyPageAndClickButton', claimType.continueButton, claimType.mainHeader);
    await performValidation('radioButtonChecked', claimantInformation.yesRadioOption, true);
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
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancyRadioOption,
      day: tenancyLicenceDetails.dayTextInput,
      month: tenancyLicenceDetails.monthTextInput,
      year: tenancyLicenceDetails.yearTextInput
    });
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.noRadioOption});
    await performAction('selectYourPossessionGrounds', {
      mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier],
      discretionary: [whatAreYourGroundsForPossession.discretionary.domesticViolence14A, whatAreYourGroundsForPossession.discretionary.rentArrears],
    });
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier,
        whatAreYourGroundsForPossession.discretionary.domesticViolence14A])
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
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption: 'Weekly', rentAmount: '800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.noRadioOption,
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
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.english
    });
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

  test('England - Resume without saved options - Secure tenancy - No Rent Arrears', async () => {
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
    await performAction('signOut');
    await performAction('reloginAndFindTheCase', user.claimantSolicitor);
    await performAction('clickButtonAndVerifyPageNavigation', resumeClaim.continue, resumeClaimOptions.mainHeader);
    await performAction('selectResumeClaimOption', resumeClaimOptions.no);
    await performValidation('radioButtonChecked', claimantType.england.registeredProviderForSocialHousing, false);
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performValidation('radioButtonChecked', claimType.noRadioOption, false);
    await performAction('selectClaimType', claimType.noRadioOption);
    await performValidation('radioButtonChecked', claimantInformation.yesRadioOption, false);
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
      tenancyOrLicenceType: tenancyLicenceDetails.secureTenancyRadioOption});
    await performValidation('mainHeader', whatAreYourGroundsForPossession.groundsForPossessionMainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture4],
      mandatory: [whatAreYourGroundsForPossession.mandatory.antiSocialBehaviour],
      mandatoryAccommodation: [whatAreYourGroundsForPossession.mandatoryWithAccommodation.charitableLandlords, whatAreYourGroundsForPossession.mandatoryWithAccommodation.landlordsWorks],
      discretionaryAccommodation: [whatAreYourGroundsForPossession.discretionaryWithAccommodation.adapted, whatAreYourGroundsForPossession.discretionaryWithAccommodation.tied],
    });
    await performValidation('mainHeader', reasonsForPossession.mainHeader);
    await performAction('enterReasonForPossession'
      , [whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture4, whatAreYourGroundsForPossession.mandatory.antiSocialBehaviour,
        whatAreYourGroundsForPossession.mandatoryWithAccommodation.charitableLandlords, whatAreYourGroundsForPossession.mandatoryWithAccommodation.landlordsWorks,
        whatAreYourGroundsForPossession.discretionaryWithAccommodation.adapted, whatAreYourGroundsForPossession.discretionaryWithAccommodation.tied
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
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byFirstClassPostOrRadioOption,
      day: '16', month: '07', year: '1985'
    });
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
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.english
    });
    await performAction('completingYourClaim', completeYourClaim.saveItForLater);
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performAction('claimSaved');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });
});

