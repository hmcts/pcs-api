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
  completeYourClaim,
  contactPreferences,
  defendantCircumstances,
  defendantDetails,
  groundsForPossession,
  home,
  languageUsed,
  mediationAndSettlement,
  noticeDetails,
  noticeOfYourIntention,
  preActionProtocol,
  propertyDetails,
  signInOrCreateAnAccount,
  statementOfTruth,
  tenancyLicenceDetails,
  underlesseeOrMortgageeEntitledToClaim,
  user,
  wantToUploadDocuments,
  whatAreYourGroundsForPossession,
  borderPostcode,
  claimType
} from '@data/page-data';
import {
  initializeExecutor,
  performAction,
  performValidation,
  performValidations
} from '@utils/controller';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { resetCYAData, resetCYAAddressData } from '@utils/actions/custom-actions/collectCYAData.action';

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
  resetCYAData();
  resetCYAAddressData();
  PageContentValidation.finaliseTest();
});

test.describe('[Create Case - England Simple Journey for CYA Validation] @regression', async () => {
  test('England - England Simple Journey for CYA Validation @PR', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandWalesCrossBorderPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('selectBorderPostcode', borderPostcode.countryOptions.england);
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader);
    await performValidation('validateCheckYourAnswersAddress', {logQA: 'Yes'});
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
      addAdditionalDefendantsOption: defendantDetails.noRadioOption});
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy});
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession',{groundsRadioInput: groundsForPossession.no});
    await performAction('selectYourPossessionGrounds',{mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet]});
    await performAction('enterReasonForPossession', [whatAreYourGroundsForPossession.mandatory.holidayLet]);
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.no,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {howDidYouServeNotice: noticeDetails.byFirstClassPost});
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {circumstanceOption: claimantCircumstances.no});
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', {defendantCircumstance: defendantCircumstances.yesRadioOption});
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession');
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
    await performAction('completingYourClaim', completeYourClaim.submitAndClaimNow);
    await performAction('selectStatementOfTruth', {
      completedBy: statementOfTruth.claimantRadioOption,
      iBelieveCheckbox: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
      fullNameTextInput: statementOfTruth.fullNameHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruth.positionOrOfficeHeldHiddenTextInput
    });
    await performValidation('validateCheckYourAnswers', {logQA: 'Yes'});
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
