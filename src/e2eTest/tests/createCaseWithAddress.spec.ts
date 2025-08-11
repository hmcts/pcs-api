import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import {
  initializeExecutor,
  performAction,
  performValidation, performValidations
} from '@utils/controller';
import configData from '@config/test.config';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {legislativeCountry} from '@data/page-data/legislativeCountry.page.data';

test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('createUserAndLogin', ['caseworker-pcs', 'caseworker']);
  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
  await performAction('clickButton', 'Create case');
  await performAction('selectCaseOptions');
});

test.describe('[Create Case Flow With Address and Claimant Type]  @Master @nightly', async () => {
  test('England - Successful case creation', async () => {
    await performAction('selectAddress', {postcode: addressDetails.englandPostcode,
      addressIndex: addressDetails.addressIndex});
    await performAction('selectLegislativeCountry', legislativeCountry.england);
    await performAction('selectClaimantType', claimantType.registeredProviderForSocialHousing);
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('clickTab', 'Property Details');
    await performValidations('address info is not null '
      , ['formLabelValue', 'Building and Street']
      , ['formLabelValue', 'Town or City']
      , ['formLabelValue', 'Postcode/Zipcode']
      , ['formLabelValue', 'Country']);
  });

  test('Wales - Successful case creation', async () => {
    await performAction('enterTestAddressManually');
    await performAction('selectLegislativeCountry', legislativeCountry.wales);
    await performAction('selectClaimantType', {claimantType : claimantType.registeredCommunityLandlord});
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('clickTab', 'Property Details');
    await performValidations('address info is not null',
      ['formLabelValue', 'Building and Street'],
      ['formLabelValue', 'Address Line 2'],
      ['formLabelValue', 'Town or City'],
      ['formLabelValue', 'Postcode/Zipcode'],
      ['formLabelValue', 'Country']);
  });

  test('England - Unsuccessful case creation journey due to claimant type not in scope of Release1', async () => {
    await performAction('selectAddress', {postcode: addressDetails.englandPostcode,
      addressIndex: addressDetails.addressIndex});
    await performAction('selectLegislativeCountry', {country: legislativeCountry.england});
    await performAction('selectClaimantType', {claimantType : claimantType.mortgageLender});
    await performValidation('mainHeader', 'You\'re not eligible for this online service');
    await performAction('clickButton', 'Close and return to case list');
  });

  test('Wales - Unsuccessful case creation journey due to claimant type not in scope of Release1', async () => {
    await performAction('selectAddress', {postcode: addressDetails.walesPostcode,
      addressIndex: addressDetails.addressIndex});
    await performAction('selectLegislativeCountry', {country: legislativeCountry.wales});
    await performAction('selectClaimantType', {claimantType : claimantType.privateLandlord});
    await performValidation('mainHeader', 'You\'re not eligible for this online service');
    await performAction('clickButton', 'Close and return to case list');
  });
});

