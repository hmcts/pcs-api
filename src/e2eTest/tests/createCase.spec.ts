import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import { initializeExecutor, performAction, performValidation, performValidations } from '@utils/controller';
import configData from '@config/test.config';
import { addressDetails } from '@data/page-data/addressDetails.page.data';
import { claimantType } from '@data/page-data/claimantType.page.data';
import { legislativeCountry } from '@data/page-data/legislativeCountry.page.data';
import { claimType } from '@data/page-data/claimType.page.data';
import { claimantName } from '@data/page-data/claimantName.page.data';
import { contactPreferences } from '@data/page-data/contactPreferences.page.data';
import { groundsForPossession } from '@data/page-data/groundsForPossession.page.data';
import { preActionProtocol } from '@data/page-data/preActionProtocol.page.data';
import { mediationAndSettlement } from '@data/page-data/mediationAndSettlement.page.data';
import { checkingNotice } from '@data/page-data/checkingNotice.page.data';
import { noticeDetails } from '@data/page-data/noticeDetails.page.data';
import { rentDetails } from '@data/page-data/rentDetails.page.data';
import { userIneligible } from '@data/page-data/userIneligible.page.data';
import { tenancyLicenceDetails } from '@data/page-data/tenancyLicenceDetails.page.data';

test.beforeEach(async ({page}, testInfo) => {
  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('createUserAndLogin', ['caseworker-pcs', 'caseworker']);
  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
  await performAction('clickButton', 'Create case');
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.describe.skip('[Create Case Flow With Address and Claimant Type]  @Master @nightly', async () => {
  test('England - Successful case creation', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('selectLegislativeCountry', legislativeCountry.england);
    await performAction('selectClaimantType', claimantType.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.yes);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.yes,
      correspondenceAddress: contactPreferences.yes,
      phoneNumber: contactPreferences.no
    });
    await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.other,
      day: tenancyLicenceDetails.day,
      month: tenancyLicenceDetails.month,
      year: tenancyLicenceDetails.year,
      files: ['sampleDocx.docx', 'samplePNG.png']
    });
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossission', groundsForPossession.yes);
    await performValidation('mainHeader', preActionProtocol.mainHeader);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performValidation('text', {"text": checkingNotice.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink"})
    await performValidation('text', {"text": checkingNotice.servedNoticeInteractiveText, "elementType": "inlineText"});
    await performAction('selectNoticeOfYourIntention', checkingNotice.yes);
    await performValidation('mainHeader', noticeDetails.mainHeader);
    await performAction('clickButton', checkingNotice.continue);
    await performAction('clickButton', noticeDetails.continue);
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('clickTab', 'Property Details');
    await performValidations(
      'address info not null',
      ['formLabelValue', 'Building and Street'],
      ['formLabelValue', 'Town or City'],
      ['formLabelValue', 'Postcode/Zipcode'],
      ['formLabelValue', 'Country']
    )
  });

  test('Wales - Successful case creation', async () => {
    await performAction('enterTestAddressManually');
    await performAction('selectLegislativeCountry', legislativeCountry.wales);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performAction('selectClaimType', claimType.no);
    await performAction('selectClaimantName', claimantName.no);
    await performAction('selectContactPreferences', {
      notifications: contactPreferences.no,
      correspondenceAddress: contactPreferences.no,
      phoneNumber: contactPreferences.yes
    });
    await performAction('selectTenancyOrLicenceDetails', {
      tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy});
    await performValidation('mainHeader', groundsForPossession.mainHeader);
    await performAction('selectGroundsForPossission', groundsForPossession.yes);
    await performAction('selectPreActionProtocol', preActionProtocol.yes);
    await performAction('selectMediationAndSettlement', {
      attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
      settlementWithDefendantsOption: mediationAndSettlement.no,
    });
    await performValidation('mainHeader', checkingNotice.mainHeader);
    await performAction('selectNoticeOfYourIntention', checkingNotice.no);
    await performValidation('mainHeader', rentDetails.mainHeader);
    await performAction('clickButton', rentDetails.continue);
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('clickTab', 'Property Details');
    await performValidations('address information entered',
      ['formLabelValue', 'Building and Street', addressDetails.buildingAndStreet],
      ['formLabelValue', 'Address Line 2', addressDetails.addressLine2],
      ['formLabelValue', 'Town or City', addressDetails.townOrCity],
      ['formLabelValue', 'Postcode/Zipcode', addressDetails.postcode],
      ['formLabelValue', 'Country', addressDetails.country]);
  });

  test('England - Unsuccessful case creation journey due to claimant type not in scope of Release1 @R1only', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('selectLegislativeCountry', legislativeCountry.england);
    await performAction('selectClaimantType', claimantType.mortgageLender);
    await performValidation('mainHeader', 'You\'re not eligible for this online service');
    await performAction('clickButton', 'Continue');
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performAction('clickButton', 'Cancel');
  });

  test('Wales - Unsuccessful case creation journey due to claimant type not in scope of Release1 @R1only', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('selectLegislativeCountry', legislativeCountry.wales);
    await performAction('selectClaimantType', claimantType.privateLandlord);
    await performValidation('mainHeader', 'You\'re not eligible for this online service');
    await performAction('clickButton', 'Continue');
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performAction('clickButton', 'Cancel');
  });

  test('Unsuccessful case creation journey due to claim type not in scope of Release1 @R1only', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('selectLegislativeCountry', legislativeCountry.england);
    await performAction('selectClaimantType', claimantType.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.yes);
    await performValidation('mainHeader', 'You\'re not eligible for this online service');
    await performAction('clickButton', 'Continue');
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performAction('clickButton', 'Cancel');
  });
});
