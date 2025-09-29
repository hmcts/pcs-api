import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import { initializeExecutor, performAction, performValidation, performValidations } from '@utils/controller';
import { addressDetails } from '@data/page-data/addressDetails.page.data';
import { claimantType } from '@data/page-data/claimantType.page.data';
import { claimType } from '@data/page-data/claimType.page.data';
import { claimantName } from '@data/page-data/claimantName.page.data';
import { contactPreferences } from '@data/page-data/contactPreferences.page.data';
import { defendantDetails } from '@data/page-data/defendantDetails.page.data';
import { tenancyLicenceDetails } from '@data/page-data/tenancyLicenceDetails.page.data';
import { groundsForPossession } from '@data/page-data/groundsForPossession.page.data';
import { rentArrearsPossessionGrounds } from '@data/page-data/rentArrearsPossessionGrounds.page.data';
import { preActionProtocol } from '@data/page-data/preActionProtocol.page.data';
import { mediationAndSettlement } from '@data/page-data/mediationAndSettlement.page.data';
import { noticeOfYourIntention } from '@data/page-data/noticeOfYourIntention.page.data';
import { noticeDetails } from '@data/page-data/noticeDetails.page.data';
import { rentDetails } from '@data/page-data/rentDetails.page.data';
import { dailyRentAmount } from '@data/page-data/dailyRentAmount.page.data';
import { provideMoreDetailsOfClaim } from '@data/page-data/provideMoreDetailsOfClaim.page.data';
import { resumeClaim } from '@data/page-data/resumeClaim.page.data';
import { resumeClaimOptions } from '@data/page-data/resumeClaimOptions.page.data';
import { detailsOfRentArrears } from '@data/page-data/detailsOfRentArrears.page.data';
import { whatAreYourGroundsForPossession } from '@data/page-data/whatAreYourGroundsForPossession.page.data';
import { rentArrearsOrBreachOfTenancy } from '@data/page-data/rentArrearsOrBreachOfTenancy.page.data';
import { reasonsForPossession } from '@data/page-data/reasonsForPossession.page.data';
import { moneyJudgment } from '@data/page-data/moneyJudgment.page.data';
import { claimantsName } from '@utils/actions/custom-actions/createCase.action';
import { claimantCircumstances } from '@data/page-data/claimantCircumstances.page.data';
import { user } from '@data/user-data/permanent.user.data';

test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('login', user.claimantSolicitor);
  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
  await performAction('clickTab', 'Create case');
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.describe('[Create Case Flow]  @Master @nightly', async () => {
  test('England - Successful case creation with Assured tenancy', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('clickButton', provideMoreDetailsOfClaim.continue);
    await performValidation('mainHeader', claimantType.mainHeader);
    await performAction('selectClaimantType', claimantType.registeredProviderForSocialHousing);
    await performValidation('mainHeader', claimType.mainHeader);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('defendantDetails', {
      name: defendantDetails.yes,
      correspondenceAddress: defendantDetails.yes,
      email: defendantDetails.yes,
      correspondenceAddressSame: defendantDetails.no
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
    await performAction('selectGroundsForPossession', groundsForPossession.yes);
    await performAction('selectRentArrearsPossessionGround', {
      rentArrears: [rentArrearsPossessionGrounds.rentArrears, rentArrearsPossessionGrounds.seriousRentArrears, rentArrearsPossessionGrounds.persistentDelayInPayingRent],
      otherGrounds: rentArrearsPossessionGrounds.yes
    });
    await performAction('selectOtherGrounds', {
      mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier],
      discretionary: [whatAreYourGroundsForPossession.discretionary.domesticViolence14A, whatAreYourGroundsForPossession.discretionary.rentArrears],
    });
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', { "text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink" })
    await performValidation('text', { "text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText" });
    await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.yes);
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byFirstClassPost,
      index: noticeDetails.byFirstClassPostIndex,
      day: '16', month: '07', year: '1985', files: 'NoticeDetails.pdf'
    });
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', { rentFrequencyOption: 'weekly', rentAmount: '800' });
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.no,
      unpaidRentAmountPerDay: '20'
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectClaimForMoney', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.yesOrno,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performAction('clickTab', 'Property Details');
    await performValidations(
      'address info not null',
      ['formLabelValue', 'Building and Street'],
      ['formLabelValue', 'Town or City'],
      ['formLabelValue', 'Postcode/Zipcode'],
      ['formLabelValue', 'Country']
    )
  });

  // The sections commented out will be fixed as part of the User Story https://tools.hmcts.net/jira/browse/HDPI-2123
  test('England - Successful case creation with Assured tenancy with No Rent arrears', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('clickButton', provideMoreDetailsOfClaim.continue);
    await performAction('selectClaimantType', claimantType.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performAction('defendantDetails', {
      name: defendantDetails.yes,
      correspondenceAddress: defendantDetails.yes,
      email: defendantDetails.yes,
      correspondenceAddressSame: defendantDetails.no
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
    await performAction('selectGroundsForPossession', groundsForPossession.no);
    await performValidation('mainHeader', whatAreYourGroundsForPossession.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier],
      discretionary: [whatAreYourGroundsForPossession.discretionary.domesticViolence14A, whatAreYourGroundsForPossession.discretionary.rentArrears]
    });
    await performValidation('mainHeader', reasonsForPossession.mainHeader);
    await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier,
      whatAreYourGroundsForPossession.discretionary.domesticViolence14A, whatAreYourGroundsForPossession.discretionary.rentArrears]);
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performValidation('text', { "text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink" })
    await performValidation('text', { "text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText" });
    await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.yes);
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byFirstClassPost,
      index: noticeDetails.byFirstClassPostIndex,
      day: '16', month: '07', year: '1985'
    });
    // await performValidation('mainHeader', rentDetails.mainHeader);
    // await performAction('provideRentDetails', { rentFrequencyOption: 'weekly', rentAmount: '800' });
    // await performValidation('mainHeader', dailyRentAmount.mainHeader);
    // await performAction('selectDailyRentAmount', {
    //   calculateRentAmount: '£114.29',
    //   unpaidRentInteractiveOption: dailyRentAmount.no,
    //   unpaidRentAmountPerDay: '20'
    // });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectClaimForMoney', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.yesOrno,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performAction('clickTab', 'Property Details');
    await performValidations(
      'address info not null',
      ['formLabelValue', 'Building and Street'],
      ['formLabelValue', 'Town or City'],
      ['formLabelValue', 'Postcode/Zipcode'],
      ['formLabelValue', 'Country']
    )
  });

  test('Wales - Successful case creation with Saved options', async () => {
    await performAction('enterTestAddressManually');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButton', provideMoreDetailsOfClaim.continue);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.no);
    await performAction('clickButton', 'Sign out');
    await performAction('reloginAndFindTheCase', user.claimantSolicitor);
    await performAction('clickButton', resumeClaim.continue);
    await performAction('selectResumeClaimOption', resumeClaimOptions.yes);
    await performValidation('radioButtonChecked', claimantType.registeredCommunityLandlord, true);
    await performAction('clickButton', 'Continue');
    await performValidation('radioButtonChecked', claimType.no, true);
    await performAction('clickButton', 'Continue');
    await performValidation('radioButtonChecked', claimantName.no, true);
    await performAction('clickButton', 'Continue');
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
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy
    });
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', groundsForPossession.yes);
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
    await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.no);
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', { rentAmount: '850', rentFrequencyOption: 'Other', inputFrequency: rentDetails.rentFrequencyFortnightly, unpaidRentAmountPerDay: '50' });
    await performValidation('mainHeader', detailsOfRentArrears.mainHeader);
    await performAction('provideDetailsOfRentArrears', {
      files: ['rentArrears.docx', 'rentArrears.pdf'],
      rentArrearsAmountOnStatement: '1000',
      rentPaidByOthersOption: detailsOfRentArrears.yes,
      paymentOptions: [detailsOfRentArrears.universalCreditOption, detailsOfRentArrears.paymentOtherOption]
    });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectClaimForMoney', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.yesOrno,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performAction('clickTab', 'Property Details');
    await performValidations('address information entered',
      ['formLabelValue', 'Building and Street', addressDetails.buildingAndStreet],
      ['formLabelValue', 'Address Line 2', addressDetails.addressLine2],
      ['formLabelValue', 'Town or City', addressDetails.townOrCity],
      ['formLabelValue', 'Postcode/Zipcode', addressDetails.walesCourtAssignedPostcode],
      ['formLabelValue', 'Country', addressDetails.country]);
  });

  // The sections commented out will be fixed as part of the User Story https://tools.hmcts.net/jira/browse/HDPI-2123
  test('Wales - Successful case creation without Saved options and Defendants correspondence address is not known with flexible tenancy', async () => {
    await performAction('enterTestAddressManually');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButton', provideMoreDetailsOfClaim.continue);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButton', 'Sign out');
    await performAction('reloginAndFindTheCase', user.claimantSolicitor);
    await performAction('clickButton', resumeClaim.continue);
    await performAction('selectResumeClaimOption', resumeClaimOptions.no);
    await performValidation('radioButtonChecked', claimantType.registeredCommunityLandlord, false);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performValidation('radioButtonChecked', claimType.no, false);
    await performAction('selectClaimType', claimType.no);
    await performValidation('radioButtonChecked', claimantName.no, true);
    await performAction('selectClaimantName', claimantName.yes);
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
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.flexibleTenancy
    });
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
    await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.no);
    // await performValidation('mainHeader', rentDetails.mainHeader);
    // await performAction('provideRentDetails', { rentFrequencyOption: 'Monthly', rentAmount: '1000' });
    // await performAction('selectDailyRentAmount', {
    //   calculateRentAmount: '£32.85',
    //   unpaidRentInteractiveOption: dailyRentAmount.yes
    // });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectClaimForMoney', moneyJudgment.yes);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.yesOrno,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performAction('clickTab', 'Property Details');
    await performValidations('address information entered',
      ['formLabelValue', 'Building and Street', addressDetails.buildingAndStreet],
      ['formLabelValue', 'Address Line 2', addressDetails.addressLine2],
      ['formLabelValue', 'Town or City', addressDetails.townOrCity],
      ['formLabelValue', 'Postcode/Zipcode', addressDetails.walesCourtAssignedPostcode],
      ['formLabelValue', 'Country', addressDetails.country]);
  });

  // The sections commented out will be fixed as part of the User Story https://tools.hmcts.net/jira/browse/HDPI-2123
  test('Wales - Successful case creation for secure tenancy type with rent and other grounds', async () => {
    await performAction('enterTestAddressManually');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButton', provideMoreDetailsOfClaim.continue);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.no);
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
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.secureTenancy
    });
    await performValidation('mainHeader', whatAreYourGroundsForPossession.mainHeader);
    await performAction('selectYourPossessionGrounds', {
      discretionary: [whatAreYourGroundsForPossession.discretionary.rentArrearsOrBreachOfTenancy, whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture],
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
      , [whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture, whatAreYourGroundsForPossession.mandatory.antiSocialBehaviour,
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
    await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.yes);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byDeliveringAtPermittedPlace,
      index: noticeDetails.byDeliveringAtPermittedPlaceIndex,
      day: '25', month: '02', year: '1970', files: 'NoticeDetails.pdf'
    });
    // await performAction('provideRentDetails', { rentFrequencyOption: 'Monthly', rentAmount: '1000' });
    // await performValidation('mainHeader', dailyRentAmount.mainHeader);
    // await performAction('selectDailyRentAmount', {
    //   calculateRentAmount: '£32.85',
    //   unpaidRentInteractiveOption: dailyRentAmount.yes
    // });
    await performValidation('mainHeader', moneyJudgment.mainHeader);
    await performAction('selectClaimForMoney', moneyJudgment.no);
    await performValidation('mainHeader', claimantCircumstances.mainHeader);
    await performAction('selectClaimantCircumstances', {
      circumstanceOption: claimantCircumstances.yesOrno,
      claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    });
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    await performAction('clickTab', 'Property Details');
    await performValidations('address information entered',
      ['formLabelValue', 'Building and Street', addressDetails.buildingAndStreet],
      ['formLabelValue', 'Address Line 2', addressDetails.addressLine2],
      ['formLabelValue', 'Town or City', addressDetails.townOrCity],
      ['formLabelValue', 'Postcode/Zipcode', addressDetails.walesCourtAssignedPostcode],
      ['formLabelValue', 'Country', addressDetails.country]);
  });
});
