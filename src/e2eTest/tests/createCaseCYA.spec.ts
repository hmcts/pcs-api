import {test} from '@playwright/test';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {addressDetails, claimantType, claimType, claimantName, contactPreferences, defendantDetails, tenancyLicenceDetails,
  groundsForPossession, preActionProtocol, mediationAndSettlement, noticeOfYourIntention, whatAreYourGroundsForPossession,
  reasonsForPossession, claimantCircumstances, applications, completeYourClaim, user, checkYourAnswers, languageUsed,
  defendantCircumstances, claimingCosts, additionalReasonsForPossession, underlesseeOrMortgageeEntitledToClaim, alternativesToPossession,
  wantToUploadDocuments, home, statementOfTruth, signInOrCreateAnAccount, addressCheckYourAnswers} from '@data/page-data';
import {JourneyDataCollector} from '@utils/cya/journey-data-collector';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  // Reset journey data collector for each test
  const collector = JourneyDataCollector.getInstance();
  collector.reset('England - Simple Journey for CYA validation');

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

test.describe('[Create Case - England] @Master @nightly', async () => {
  test('England - Simple Journey for CYA validation', async () => {
    // Address selection - data collection happens automatically in action
    await performAction('selectAddress', {
      postcode: addressDetails.englandWalesCrossBorderPostcode,
      addressIndex: addressDetails.addressIndex
    });

    // Validate Address CYA page - automatically compares collected address data with displayed data
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader);
    await performValidation('validateCYA', '');
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');

    // All data collection happens automatically in actions - no manual collection needed
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);

    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });

    await performAction('defendantDetails', {
      name: defendantDetails.no,
      correspondenceAddress: defendantDetails.no,
      email: defendantDetails.no,
      correspondenceAddressSame: defendantDetails.no
    });

    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy});

    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession',{groundsRadioInput: groundsForPossession.no});

    await performValidation('mainHeader', whatAreYourGroundsForPossession.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      mandatory : [whatAreYourGroundsForPossession.mandatory.holidayLet],
      discretionary :[whatAreYourGroundsForPossession.discretionary.domesticViolence14A]
    });

    await performValidation('mainHeader', reasonsForPossession.mainHeader);
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.discretionary.domesticViolence14A]);

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
      option: noticeOfYourIntention.no
    });

    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {circumstanceOption: claimantCircumstances.no});

    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', defendantCircumstances.no);

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
    await performAction('clickButton', statementOfTruth.continue);

    // Validate CYA page - automatically compares collected data with displayed data
    await performValidation('mainHeader', checkYourAnswers.header);
    await performValidation('validateCYA', '');

    await performAction('clickButton', checkYourAnswers.saveAndContinue);
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
  });
});
