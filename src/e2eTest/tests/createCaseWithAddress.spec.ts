import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {
  initializeExecutor,
  performAction,
  performActions,
  performValidation, performValidations,
} from '@utils/controller';
import configData from "@config/test.config";
import {headers} from "@data/header.data";
import {caseOption} from "@data/page-data/caseOptions.data";
import {addressDetails} from "@data/page-data/addressDetails.data";
import {claimantTypeOptions, legislativeCountry} from "@data/page-data/claimantType.data";


test.beforeEach(async ({page}, testInfo) => {

  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('login', 'exuiUser');

  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
});

test.describe('[Create Case Flow With Address and Claimant Type]  @Master @nightly', async () => {

  test('England - Successful case creation', async () => {

    await performAction('clickButton', 'Create case');
    await performValidation('mainHeader', {expected: headers.createCase});
    await performActions('Case option selection'
      , ['select', 'Jurisdiction', caseOption.jurisdiction.posessions]
      , ['select', 'Case type', caseOption.caseType.civilPosessions]
      , ['select', 'Event', caseOption.event.makeAPosessionClaim]);
    await performAction('clickButton', 'Start');
    await performValidation('mainHeader', {expected: headers.whatIsTheAddressOfThePropertyYouAreClaimingPossessionOf});
    await performActions('Find Address based on postcode'
      , ['inputText', 'Enter a UK postcode', addressDetails.englandPostcode]
      , ['clickButton', 'Find address']
      , ['select', 'Select an address', addressDetails.addressIndex]
      , ['inputText', 'Address Line 2', addressDetails.addressLine2]
      , ['inputText', 'Address Line 3', addressDetails.addressLine3]
      , ['inputText', 'County', addressDetails.englandCounty]);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', legislativeCountry.options.england);
    await performAction('clickButton', 'Continue');
    await performValidation('mainHeader', {expected: headers.claimantType});
    await performAction('clickButton', 'Continue');
    await performValidation('errorMessage', {
      header: 'There is a problem',
      errorHasLink: 'Who is the claimant in this case? is required',
    });
    await performValidations('address info is not null '
      , ['visibility', claimantTypeOptions.england.privateLandlord]
      , ['visibility', claimantTypeOptions.england.registeredProviderForSocialHousing]
      , ['visibility', claimantTypeOptions.england.mortgageProviderOrLender]
      , ['visibility', claimantTypeOptions.england.other]);
    await performAction('clickRadioButton', claimantTypeOptions.england.registeredProviderForSocialHousing);
    await performAction('clickButton', 'Continue');
    await performAction('clickButton', 'Save and continue');
    await performValidation("bannerAlert", {message: "Case #.* has been created."});
    await performAction('clickTab', 'Property Details');
    await performValidations('address info is not null '
      , ['formLabelValue', 'Building and Street']
      , ['formLabelValue', 'Address Line 2']
      , ['formLabelValue', 'Address Line 2']
      , ['formLabelValue', 'Postcode/Zipcode']
      , ['formLabelValue', 'Country']);
  });

  test('Wales - Successful case creation', async () => {
    await performAction('clickButton', 'Create case');
    await performActions('Case option selection'
      , ['select', 'Jurisdiction', caseOption.jurisdiction.posessions]
      , ['select', 'Case type', caseOption.caseType.civilPosessions]
      , ['select', 'Event', caseOption.event.makeAPosessionClaim]);
    await performAction('clickButton', 'Start');
    await performActions('Enter Address Manually'
      , ['clickButton', "I can't enter a UK postcode"]
      , ['inputText', 'Building and Street', addressDetails.BuildingAndStreet]
      , ['inputText', 'Address Line 2', addressDetails.addressLine2]
      , ['inputText', 'Address Line 3', addressDetails.addressLine3]
      , ['inputText', 'Town or City', addressDetails.TownOrCity]
      , ['inputText', 'County', addressDetails.walesCounty]
      , ['inputText', 'Postcode/Zipcode', addressDetails.walesPostcode]
      , ['inputText', 'Country', addressDetails.country]);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', legislativeCountry.options.wales);
    await performAction('clickButton', 'Continue');
    await performValidations('address info is not null '
      , ['visibility', claimantTypeOptions.wales.privateLandlord]
      , ['visibility', claimantTypeOptions.wales.registeredCommunityLandlord]
      , ['visibility', claimantTypeOptions.wales.mortgageLender]
      , ['visibility', claimantTypeOptions.wales.other]);
    await performAction('clickRadioButton', claimantTypeOptions.wales.registeredCommunityLandlord);
    await performAction('clickButton', 'Continue');
    await performAction('clickButton', 'Save and continue');
    await performValidation("bannerAlert", {message: "Case #.* has been created."});
    await performAction('clickTab', 'Property Details');
    await performValidations('address info is not null',
      ['formLabelValue', 'Building and Street'],
      ['formLabelValue', 'Address Line 2'],
      ['formLabelValue', 'Town or City'],
      ['formLabelValue', 'Postcode/Zipcode'],
      ['formLabelValue', 'Country']);
  });

  test('England - Unsuccessful case creation journey due to claimant type not in scope of Release1', async () => {

    await performAction('clickButton', 'Create case');
    await performValidation('mainHeader', {expected: headers.createCase});
    await performActions('Case option selection'
      , ['select', 'Jurisdiction', caseOption.jurisdiction.posessions]
      , ['select', 'Case type', caseOption.caseType.civilPosessions]
      , ['select', 'Event', caseOption.event.makeAPosessionClaim]);
    await performAction('clickButton', 'Start');
    await performValidation('mainHeader', {expected: headers.whatIsTheAddressOfThePropertyYouAreClaimingPossessionOf});
    await performActions('Find Address based on postcode'
      , ['inputText', 'Enter a UK postcode', addressDetails.englandPostcode]
      , ['clickButton', 'Find address']
      , ['select', 'Select an address', addressDetails.addressIndex]
      , ['inputText', 'Address Line 2', addressDetails.addressLine2]
      , ['inputText', 'Address Line 3', addressDetails.addressLine3]
      , ['inputText', 'County', addressDetails.englandCounty]);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', legislativeCountry.options.england);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', claimantTypeOptions.england.privateLandlord);
    await performAction('clickButton', 'Continue');
    await performValidation('mainHeader', {expected: "You're not eligible for this online service"});
    await performAction('clickButton', 'Close and return to case list');

  });

  test('Wales - Unsuccessful case creation journey due to claimant type not in scope of Release1', async () => {

    await performAction('clickButton', 'Create case');
    await performValidation('mainHeader', {expected: headers.createCase});
    await performActions('Case option selection'
      , ['select', 'Jurisdiction', caseOption.jurisdiction.posessions]
      , ['select', 'Case type', caseOption.caseType.civilPosessions]
      , ['select', 'Event', caseOption.event.makeAPosessionClaim]);
    await performAction('clickButton', 'Start');
    await performValidation('mainHeader', {expected: headers.whatIsTheAddressOfThePropertyYouAreClaimingPossessionOf});
    await performActions('Find Address based on postcode'
      , ['inputText', 'Enter a UK postcode', addressDetails.walesPostcode]
      , ['clickButton', 'Find address']
      , ['select', 'Select an address', addressDetails.addressIndex]
      , ['inputText', 'Address Line 2', addressDetails.addressLine2]
      , ['inputText', 'Address Line 3', addressDetails.addressLine3]
      , ['inputText', 'County', addressDetails.walesCounty]);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', legislativeCountry.options.wales);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', claimantTypeOptions.wales.privateLandlord);
    await performAction('clickButton', 'Continue');
    await performValidation('mainHeader', {expected: "You're not eligible for this online service"});
    await performAction('clickButton', 'Close and return to case list');
  });

});
