import {test} from '@playwright/test';
import {initializeExecutor, performAction, performValidation, performValidations} from '@utils/controller';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {claimType} from '@data/page-data/claimType.page.data';
import {claimantName} from '@data/page-data/claimantName.page.data';
import {contactPreferences} from '@data/page-data/contactPreferences.page.data';
import {defendantDetails} from '@data/page-data/defendantDetails.page.data';
import {tenancyLicenceDetails} from '@data/page-data/tenancyLicenceDetails.page.data';
import {groundsForPossession} from '@data/page-data/groundsForPossession.page.data';
import {rentArrearsPossessionGrounds} from '@data/page-data/rentArrearsPossessionGrounds.page.data';
import {preActionProtocol} from '@data/page-data/preActionProtocol.page.data';
import {mediationAndSettlement} from '@data/page-data/mediationAndSettlement.page.data';
import {noticeOfYourIntention} from '@data/page-data/noticeOfYourIntention.page.data';
import {noticeDetails} from '@data/page-data/noticeDetails.page.data';
import {rentDetails} from '@data/page-data/rentDetails.page.data';
import {dailyRentAmount} from '@data/page-data/dailyRentAmount.page.data';
import {whatAreYourGroundsForPossession} from '@data/page-data/whatAreYourGroundsForPossession.page.data';
import {moneyJudgment} from '@data/page-data/moneyJudgment.page.data';
import {claimantCircumstances} from '@data/page-data/claimantCircumstances.page.data';
import {applications} from '@data/page-data/applications.page.data';
import {completeYourClaim} from '@data/page-data/completeYourClaim.page.data';
import {user} from '@data/user-data/permanent.user.data';
import {checkYourAnswers} from '@data/page-data/checkYourAnswers.page.data';
import {propertyDetails} from '@data/page-data/propertyDetails.page.data';
import {languageUsed} from '@data/page-data/languageUsed.page.data';
import {defendantCircumstances} from '@data/page-data/defendantCircumstances.page.data';
import {claimingCosts} from '@data/page-data/claimingCosts.page.data';
import {uploadAdditionalDocs} from '@data/page-data/uploadAdditionalDocs.page.data';
import {statementOfTruth} from '@data/page-data/statementOfTruth.page.data';
import {home} from '@data/page-data/home.page.data';
import {additionalReasonsForPossession} from '@data/page-data/additionalReasonsForPossession.page.data';
import {underlesseeOrMortgageeEntitledToClaim} from '@data/page-data/underlesseeOrMortgageeEntitledToClaim.page.data';
import {alternativesToPossession} from '@data/page-data/alternativesToPossession.page.data';
import {wantToUploadDocuments} from '@data/page-data/wantToUploadDocuments.page.data';
import {resumeClaim} from '@data/page-data/resumeClaim.page.data';
import {resumeClaimOptions} from '@data/page-data/resumeClaimOptions.page.data';
import {signInOrCreateAnAccount} from '@data/page-data/signInOrCreateAnAccount.page.data';

// This test validates the resume & find case functionality with and without saved options.
// It is not intended to reuse for any of the e2e scenarios, those should still be covered in others specs.
// When a new page is added/flow changes, basic conditions in this test should be updated accordingly to continue the journey.
// Due to frequent issues with relogin and “Find Case” (Elasticsearch), this test is made optional only for the pipeline to maintain a green build.
// However, it must be executed locally, and evidence of the passed results should be provided during PR review in case its failing in pipeline.

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

test.describe('[Create Case - With resume claim options] @Master @nightly', async () => {
  test('England - Resume with saved options', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('signOut');
    await performAction('reloginAndFindTheCase', user.claimantSolicitor);
    await performAction('clickButtonAndVerifyPageNavigation', resumeClaim.continue, resumeClaimOptions.mainHeader);
    await performAction('selectResumeClaimOption', resumeClaimOptions.yes);
    await performValidation('radioButtonChecked', claimantType.england.registeredProviderForSocialHousing, true);
    await performAction('verifyPageAndClickButton', claimantType.continue, claimantType.mainHeader);
    await performValidation('radioButtonChecked', claimType.no, true);
    await performAction('verifyPageAndClickButton', claimType.continue, claimType.mainHeader);
    await performValidation('radioButtonChecked', claimantName.yes, true);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yes, firstName: defendantDetails.firstNameInput, lastName: defendantDetails.lastNameInput,
      correspondenceAddressOption: defendantDetails.yes, correspondenceAddressSameOption: defendantDetails.no, address: addressDetails.englandCourtAssignedPostcode,
      addAdditionalDefendantsOption: defendantDetails.no
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy,
      day: tenancyLicenceDetails.day,
      month: tenancyLicenceDetails.month,
      year: tenancyLicenceDetails.year,
      files: ['tenancyLicence.docx', 'tenancyLicence.png']
    });
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yes});
    await performAction('selectRentArrearsPossessionGround', {
      rentArrears: [rentArrearsPossessionGrounds.rentArrears, rentArrearsPossessionGrounds.seriousRentArrears, rentArrearsPossessionGrounds.persistentDelayInPayingRent],
      otherGrounds: rentArrearsPossessionGrounds.yes
    });
    await performAction('selectYourPossessionGrounds', {
      mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier],
      discretionary: [whatAreYourGroundsForPossession.discretionary.domesticViolence14A, whatAreYourGroundsForPossession.discretionary.rentArrears],
    });
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier,
        whatAreYourGroundsForPossession.discretionary.domesticViolence14A])
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', {
      "text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink,
      "elementType": "paragraphLink"
    })
    await performValidation('text', {
      "text": noticeOfYourIntention.servedNoticeInteractiveText,
      "elementType": "inlineText"
    });
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byFirstClassPost,
      day: '16', month: '07', year: '1985', files: 'NoticeDetails.pdf'
    });
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption: 'weekly', rentAmount: '800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.no,
      unpaidRentAmountPerDay: '20'
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.yes,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', defendantCircumstances.yes);
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession');
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yes);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('clickButton', underlesseeOrMortgageeEntitledToClaim.continue);
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
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.english
    });
    await performAction('completingYourClaim', completeYourClaim.submitAndClaimNow);
    await performAction('clickButton', statementOfTruth.continue);
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

  test('England - Resume without saved options', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('signOut');
    await performAction('reloginAndFindTheCase', user.claimantSolicitor);
    await performAction('clickButtonAndVerifyPageNavigation', resumeClaim.continue, resumeClaimOptions.mainHeader);
    await performAction('selectResumeClaimOption', resumeClaimOptions.no);
    await performValidation('radioButtonChecked', claimantType.england.registeredProviderForSocialHousing, false);
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performValidation('radioButtonChecked', claimType.no, false);
    await performAction('selectClaimType', claimType.no);
    await performValidation('radioButtonChecked', claimantName.yes, false);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('addDefendantDetails', {
      nameOption: defendantDetails.yes, firstName: defendantDetails.firstNameInput, lastName: defendantDetails.lastNameInput,
      correspondenceAddressOption: defendantDetails.yes, correspondenceAddressSameOption: defendantDetails.no, address: addressDetails.walesCourtAssignedPostcode,
      addAdditionalDefendantsOption: defendantDetails.no
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy,
      day: tenancyLicenceDetails.day,
      month: tenancyLicenceDetails.month,
      year: tenancyLicenceDetails.year,
      files: ['tenancyLicence.docx', 'tenancyLicence.png']
    });
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', {groundsRadioInput: groundsForPossession.yes});
    await performAction('selectRentArrearsPossessionGround', {
      rentArrears: [rentArrearsPossessionGrounds.rentArrears, rentArrearsPossessionGrounds.seriousRentArrears, rentArrearsPossessionGrounds.persistentDelayInPayingRent],
      otherGrounds: rentArrearsPossessionGrounds.yes
    });
    await performAction('selectYourPossessionGrounds', {
      mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier],
      discretionary: [whatAreYourGroundsForPossession.discretionary.domesticViolence14A, whatAreYourGroundsForPossession.discretionary.rentArrears],
    });
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier,
        whatAreYourGroundsForPossession.discretionary.domesticViolence14A])
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', {
      "text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink,
      "elementType": "paragraphLink"
    })
    await performValidation('text', {
      "text": noticeOfYourIntention.servedNoticeInteractiveText,
      "elementType": "inlineText"
    });
    await performAction('selectNoticeOfYourIntention', {
      question: noticeOfYourIntention.servedNoticeInteractiveText,
      option: noticeOfYourIntention.yes
    });
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byFirstClassPost,
      day: '16', month: '07', year: '1985', files: 'NoticeDetails.pdf'
    });
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption: 'weekly', rentAmount: '800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.no,
      unpaidRentAmountPerDay: '20'
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.yes,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', defendantCircumstances.yes);
    await performValidation('mainHeader', alternativesToPossession.mainHeader);
    await performAction('selectAlternativesToPossession');
    await performValidation('mainHeader', claimingCosts.mainHeader);
    await performAction('selectClaimingCosts', claimingCosts.yes);
    await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yes);
    await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    await performAction('clickButton', underlesseeOrMortgageeEntitledToClaim.continue);
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
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.english
    });
    await performAction('completingYourClaim', completeYourClaim.submitAndClaimNow);
    await performAction('clickButton', statementOfTruth.continue);
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

