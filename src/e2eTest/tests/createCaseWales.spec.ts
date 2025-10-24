import {test} from '@playwright/test';
import {initializeExecutor, performAction, performValidation, performValidations} from '@utils/controller';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {claimType} from '@data/page-data/claimType.page.data';
import {claimantName} from '@data/page-data/claimantName.page.data';
import {claimantDetailsWales} from '@data/page-data/claimantDetailsWales.page.data';
import {contactPreferences} from '@data/page-data/contactPreferences.page.data';
import {defendantDetails} from '@data/page-data/defendantDetails.page.data';
import {tenancyLicenceDetails} from '@data/page-data/tenancyLicenceDetails.page.data';
import {groundsForPossession} from '@data/page-data/groundsForPossession.page.data';
import {rentArrearsPossessionGrounds} from '@data/page-data/rentArrearsPossessionGrounds.page.data';
import {preActionProtocol} from '@data/page-data/preActionProtocol.page.data';
import {mediationAndSettlement} from '@data/page-data/mediationAndSettlement.page.data';
import {noticeOfYourIntention} from '@data/page-data/noticeOfYourIntention.page.data';
import {rentDetails} from '@data/page-data/rentDetails.page.data';
import {provideMoreDetailsOfClaim} from '@data/page-data/provideMoreDetailsOfClaim.page.data';
import {resumeClaim} from '@data/page-data/resumeClaim.page.data';
import {resumeClaimOptions} from '@data/page-data/resumeClaimOptions.page.data';
import {detailsOfRentArrears} from '@data/page-data/detailsOfRentArrears.page.data';
import {whatAreYourGroundsForPossession} from '@data/page-data/whatAreYourGroundsForPossession.page.data';
import {rentArrearsOrBreachOfTenancy} from '@data/page-data/rentArrearsOrBreachOfTenancy.page.data';
import {reasonsForPossession} from '@data/page-data/reasonsForPossession.page.data';
import {moneyJudgment} from '@data/page-data/moneyJudgment.page.data';
import {claimantCircumstances} from '@data/page-data/claimantCircumstances.page.data';
import {applications} from '@data/page-data/applications.page.data';
import {completeYourClaim} from '@data/page-data/completeYourClaim.page.data';
import {user} from '@data/user-data/permanent.user.data';
import {reasonsForRequestingASuspensionOrder} from '@data/page-data/reasonsForRequestingASuspensionOrder.page.data';
import {checkYourAnswers} from '@data/page-data/checkYourAnswers.page.data';
import {propertyDetails} from '@data/page-data/propertyDetails.page.data';
import {languageUsed} from '@data/page-data/languageUsed.page.data';
import {defendantCircumstances} from '@data/page-data/defendantCircumstances.page.data';
import {claimingCosts} from '@data/page-data/claimingCosts.page.data';
import {home} from '@data/page-data/home.page.data';
import {additionalReasonsForPossession} from '@data/page-data/additionalReasonsForPossession.page.data';
import {underlesseeOrMortgageeEntitledToClaim} from '@data/page-data/underlesseeOrMortgageeEntitledToClaim.page.data';
import {alternativesToPossession} from '@data/page-data/alternativesToPossession.page.data';
import {housingAct} from '@data/page-data/housingAct.page.data';
import {reasonsForRequestingADemotionOrder} from '@data/page-data/reasonsForRequestingADemotionOrder.page.data';
import {statementOfExpressTerms} from '@data/page-data/statementOfExpressTerms.page.data';
import {wantToUploadDocuments} from '@data/page-data/wantToUploadDocuments.page.data';
import {whatAreYourGroundsForPossessionWales} from '@data/page-data/whatAreYourGroundsForPossessionWales.page.data';
import {reasonsForRequestingASuspensionAndDemotionOrder} from '@data/page-data/reasonsForRequestingASuspensionAndDemotionOrder.page.data';
import {signInOrCreateAnAccount} from '@data/page-data/signInOrCreateAnAccount.page.data';

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
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
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
    //Following line can be updated to securecontractLicenceDetails once HDPI-2365 is done
    await performAction('check', 'Secure contract');
    await performAction('clickButton', 'Continue');
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
    // following code can be removed once wales journey routing is done HDPI-2365
    /* await performValidation('mainHeader', rentDetails.mainHeader);
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
     await performAction('selectDefendantCircumstances', defendantCircumstances.no);
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

  // The sections commented out will be fixed as part of the User Story https://tools.hmcts.net/jira/browse/HDPI-2123
  test('Wales - Standard contract', async () => {
    await performAction('enterTestAddressManually');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, claimantDetailsWales.mainHeader);
    await performAction('clickButton', 'Sign out');
    await performAction('reloginAndFindTheCase', user.claimantSolicitor);
    await performAction('clickButtonAndVerifyPageNavigation', resumeClaim.continue, resumeClaimOptions.mainHeader);
    await performAction('selectResumeClaimOption', resumeClaimOptions.no);
    await performValidation('radioButtonChecked', claimantType.wales.communityLandlord, false);
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performValidation('radioButtonChecked', claimType.no, false);
    await performAction('selectClaimType', claimType.no);
    await performValidation('radioButtonChecked', claimantName.no, false);
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
    //Following 186 to 188 needs update once routing is done for wales journey HDPI-2365
    await performAction('check', 'Secure contract');
    await performAction('clickButton', 'Continue');
    await performValidation('mainHeader', whatAreYourGroundsForPossessionWales.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossessionWales.discretionary.estateManagementGrounds],
      discretionaryEstateGrounds: [whatAreYourGroundsForPossessionWales.discretionary.buildingWorks],
      mandatory: [whatAreYourGroundsForPossessionWales.mandatory.section191],
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
    // following code can be removed once wales journey routing is done HDPI-2365
    /*// await performValidation('mainHeader', rentDetails.mainHeader);
    // await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    // await performAction('selectDailyRentAmount', {
    //   calculateRentAmount: '£32.85',
    //   unpaidRentInteractiveOption: dailyRentAmount.yes
    // });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', defendantCircumstances.no);
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

  // The sections commented out will be fixed as part of the User Story https://tools.hmcts.net/jira/browse/HDPI-2123
  test('Wales - Occupation contract Licence Details - Other', async () => {
    await performAction('enterTestAddressManually');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
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
    await performAction('check', 'Secure contract');
    await performAction('clickButton', 'Continue');
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
    //selectNoticeDetails has been commented out and will be modified as part of https://tools.hmcts.net/jira/browse/HDPI-2515 + https://tools.hmcts.net/jira/browse/HDPI-2516
    // await performAction('selectNoticeDetails', {
    //   howDidYouServeNotice: noticeDetails.byOtherElectronicMethod,
    //   day: '25', month: '02', year: '1970', hour: '22', minute: '45', second: '10', files: 'NoticeDetails.pdf'});
    // await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    // await performValidation('mainHeader', dailyRentAmount.mainHeader);
    // await performAction('selectDailyRentAmount', {
    //   calculateRentAmount: '£32.85',
    //   unpaidRentInteractiveOption: dailyRentAmount.yes
    // });
    // Commenting below code as tickets are yet to be implemented for wales journey
    /*await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectMoneyJudgment', moneyJudgment.no);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.no,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performValidation('mainHeader', defendantCircumstances.mainHeader);
    await performAction('selectDefendantCircumstances', defendantCircumstances.yes);
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
