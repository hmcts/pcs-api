import { test } from '@playwright/test';
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
  languageUsed,
  mediationAndSettlement,
  moneyJudgment,
  noticeDetails,
  noticeOfYourIntention,
  preActionProtocol,
  propertyDetails,
  rentArrearsPossessionGrounds,
  rentDetails,
  signInOrCreateAnAccount,
  statementOfTruth,
  tenancyLicenceDetails,
  underlesseeOrMortgageeEntitledToClaim,
  uploadAdditionalDocs,
  user,
  wantToUploadDocuments,
  whatAreYourGroundsForPossession,
  borderPostcode
} from '@data/page-data';
import {
  initializeExecutor,
  performAction,
  performValidation,
  performValidations
} from '@utils/controller';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { resetCYAData } from '@utils/data/cya-data';

test.beforeEach(async ({page}) => {
  resetCYAData(); // Reset CYA data at the start of each test
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

test.describe('[Create Case - England Simple Journey for CYA Validation] @regression', async () => {
  test('England - England Simple Journey for CYA Validation @PR @CYA', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandWalesCrossBorderPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('selectBorderPostcode', borderPostcode.countryOptions.england);
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
      files: ['tenancyLicence.docx', 'tenancyLicence.png']
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
      completedBy: statementOfTruth.claimantRadioOption,
      iBelieveCheckbox: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
      fullNameTextInput: statementOfTruth.fullNameHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruth.positionOrOfficeHeldHiddenTextInput
    });
    await performValidation('validateCheckYourAnswers');
    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performValidations(
      'address info not null',
      ['formLabelValue', propertyDetails.buildingAndStreetLabel],
      ['formLabelValue', propertyDetails.townOrCityLabel],
      ['formLabelValue', propertyDetails.postcodeZipcodeLabel],
      ['formLabelValue', propertyDetails.countryLabel],
    )
  });
});
