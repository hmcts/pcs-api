import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {initializeExecutor, performAction, performValidation, performValidations} from '@utils/controller';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {claimType} from '@data/page-data/claimType.page.data';
import {claimantName} from '@data/page-data/claimantName.page.data';
import {contactPreferences} from '@data/page-data/contactPreferences.page.data';
import {defendant1Details, defendant2Details} from '@data/page-data/defendantDetails.page.data';
import {tenancyLicenceDetails} from '@data/page-data/tenancyLicenceDetails.page.data';
import {groundsForPossession} from '@data/page-data/groundsForPossession.page.data';
import {rentArrearsPossessionGrounds} from '@data/page-data/rentArrearsPossessionGrounds.page.data';
import {whatAreYourGrounds} from '@data/page-data/mandatoryAndDiscretionaryGrounds.page.data';
import {preActionProtocol} from '@data/page-data/preActionProtocol.page.data';
import {mediationAndSettlement} from '@data/page-data/mediationAndSettlement.page.data';
import {noticeOfYourIntention} from '@data/page-data/noticeOfYourIntention.page.data';
import {noticeDetails} from '@data/page-data/noticeDetails.page.data';
import {rentDetails} from '@data/page-data/rentDetails.page.data';
import {dailyRentAmount} from '@data/page-data/dailyRentAmount.page.data';
import {provideMoreDetailsOfClaim} from '@data/page-data/provideMoreDetailsOfClaim.page.data';
import {resumeClaim} from '@data/page-data/resumeClaim.page.data';
import {resumeClaimOptions} from '@data/page-data/resumeClaimOptions.page.data';
import {detailsOfRentArrears} from '@data/page-data/detailsOfRentArrears.page.data';
import {defendantList} from '@data/page-data/defendantList.page.data';

test.beforeEach(async ({page}, testInfo) => {
  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('createUserAndLogin', 'claimant', ['caseworker-pcs', 'caseworker']);
  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
  await performAction('clickTab', 'Create case');
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.describe('[Create Case Flow With Address and Claimant Type] @Master @nightly', async () => {
  test('England - Successful case creation', async () => {
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
    await performValidation('mainHeader', defendant1Details.mainHeader);
    await performAction('defendantDetails', {
      name: defendant1Details.yes,
      firstName: defendant1Details.defendant1FirstNameInput,
      lastName: defendant1Details.defendant1LastNameInput,
      emailId: defendant1Details.defendant1EmailIdInput,
      correspondenceAddress: defendant1Details.yes,
      email: defendant1Details.yes,
      correspondenceAddressSame: defendant1Details.no,
      postcode: defendant1Details.defendant1PostcodeInput
    });
    await performValidation('mainHeader', defendantList.mainHeader);
    await performAction('addAnotherDefendant', defendantList.yes);
    await performValidation('mainHeader', defendant2Details.mainHeader);
    await performAction('defendantDetails', {
      name: defendant2Details.yes,
      firstName: defendant2Details.defendant2FirstNameInput,
      lastName: defendant2Details.defendant2LastNameInput,
      emailId: defendant2Details.defendant2EmailIdInput,
      correspondenceAddress: defendant2Details.yes,
      email: defendant2Details.yes,
      correspondenceAddressSame: defendant2Details.no,
      postcode: defendant2Details.defendant2PostcodeInput
    });
    await performAction('addAnotherDefendant', defendantList.no);
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.other,
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
    await performAction('selectOtherGrounds',{
      mandatory: [whatAreYourGrounds.mandatory.holidayLet,whatAreYourGrounds.mandatory.ownerOccupier],
      discretionary: [whatAreYourGrounds.discretionary.domesticViolence,whatAreYourGrounds.discretionary.rentArrears],
    })
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
    await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.yes);
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byFirstClassPost,
      index: noticeDetails.byFirstClassPostIndex,
      day: '16', month: '07', year: '1985'});
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('provideRentDetails', {rentFrequencyOption:'weekly', rentAmount:'800'});
    await performValidation('mainHeader', dailyRentAmount.mainHeader);
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£114.29',
      unpaidRentInteractiveOption: dailyRentAmount.no,
      unpaidRentAmountPerDay: '20'
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

  //A houskeeping ticket has been created HDPI-1952 to fix this test
  test.skip('Wales - Successful case creation with Saved options', async () => {
    await performAction('enterTestAddressManually');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButton', provideMoreDetailsOfClaim.continue);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.no);
    await performAction('clickButton', 'Sign out');
    await performAction('reloginAndFindTheCase');
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
      name: defendant1Details.yes,
      correspondenceAddress: defendant1Details.yes,
      email: defendant1Details.yes,
      correspondenceAddressSame: defendant1Details.yes
    });
    await performAction('addAnotherDefendant', defendantList.no);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy});
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
    await performAction('provideRentDetails', {rentAmount:'850', rentFrequencyOption:'Other', inputFrequency:rentDetails.rentFrequencyFortnightly,unpaidRentAmountPerDay:'50'});
    await performValidation('mainHeader', detailsOfRentArrears.mainHeader);
    await performAction('clickButton', detailsOfRentArrears.continue);
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

  //A houskeeping ticket has been created HDPI-1952 to fix this test
  test.skip('Wales - Successful case creation without Saved options and Defendants correspondence address is not known', async () => {
    await performAction('enterTestAddressManually');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButton', provideMoreDetailsOfClaim.continue);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('clickButton', 'Sign out');
    await performAction('reloginAndFindTheCase');
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
      name: defendant1Details.no,
      correspondenceAddress: defendant1Details.no,
      email: defendant1Details.no,
    });
    await performAction('addAnotherDefendant', defendantList.no)
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy});
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossession', groundsForPossession.no);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.yes);
    await performAction('selectNoticeDetails', {
      howDidYouServeNotice: noticeDetails.byDeliveringAtPermittedPlace,
      index: noticeDetails.byDeliveringAtPermittedPlaceIndex,
      day: '25', month: '02', year: '1970'});
    await performAction('provideRentDetails', {rentFrequencyOption: 'Monthly', rentAmount: '1000'});
    await performAction('selectDailyRentAmount', {
      calculateRentAmount: '£32.85',
      unpaidRentInteractiveOption: dailyRentAmount.yes
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
