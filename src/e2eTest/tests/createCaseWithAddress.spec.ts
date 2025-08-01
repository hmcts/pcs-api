import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {
  initializeExecutor,
  performAction,
  performActions,
  performValidation, performValidations,
} from '@utils/controller';
import configData from "@config/test.config";
import {createCase} from "@data/page-data/createCase.page.data";
import {addressDetails} from "@data/page-data/addressDetails.page.data";
import {claimantType} from "@data/page-data/claimantType.page.data";
import {legislativeCountry} from "@data/page-data/legislativeCountry.page.data";


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

test.describe.skip('[Create Case Flow With Address and Claimant Type]  @Master @nightly', async () => {

  test('England - Successful case creation', async () => {

    await performAction('clickButton', 'Create case');
    await performValidation('mainHeader', createCase.mainHeader);
    await performActions('Case option selection'
        , ['select', 'Jurisdiction', createCase.caseOption.jurisdiction.possessions]
        , ['select', 'Case type', createCase.caseOption.caseType.civilPossessions]
        , ['select', 'Event', createCase.caseOption.event.makeAPossessionClaim]);
    await performAction('clickButton', 'Start');
    await performValidation('mainHeader', addressDetails.mainHeader);
    await performAction('clickButton', 'Continue');
    await performValidation('errorMessage', addressDetails.errorMessage.errorMessage);
    await performActions('Find Address based on postcode'
        , ['inputText', 'Enter a UK postcode', addressDetails.propertyAddressSection.englandPostcode]
        , ['clickButton', 'Find address']
        , ['select', 'Select an address', addressDetails.propertyAddressSection.addressIndex]
        , ['inputText', 'Address Line 2', addressDetails.propertyAddressSection.addressLine2]
        , ['inputText', 'Address Line 3', addressDetails.propertyAddressSection.addressLine3]
        , ['inputText', 'County', addressDetails.propertyAddressSection.englandCounty]);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', legislativeCountry.countryOptions.england);
    await performAction('clickButton', 'Continue');
    await performValidation('mainHeader',  claimantType.mainHeader);
    await performAction('clickButton', 'Continue');
    await performValidation('errorMessage', claimantType.errorMessage.errorMessage);
    await performValidations('address info is not null '
        , ['visibility', claimantType.claimantTypeOptions.england.privateLandlord]
        , ['visibility', claimantType.claimantTypeOptions.england.registeredProviderForSocialHousing]
        , ['visibility', claimantType.claimantTypeOptions.england.mortgageProviderOrLender]
        , ['visibility', claimantType.claimantTypeOptions.england.other]);
    await performAction('clickRadioButton', claimantType.claimantTypeOptions.england.registeredProviderForSocialHousing);
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
      , ['select', 'Jurisdiction', createCase.caseOption.jurisdiction.possessions]
      , ['select', 'Case type', createCase.caseOption.caseType.civilPossessions]
      , ['select', 'Event', createCase.caseOption.event.makeAPossessionClaim]);
    await performAction('clickButton', 'Start');
    await performActions('Enter Address Manually'
      , ['clickButton', "I can't enter a UK postcode"]
      , ['inputText', 'Building and Street', addressDetails.propertyAddressSection.BuildingAndStreet]
      , ['inputText', 'Address Line 2', addressDetails.propertyAddressSection.addressLine2]
      , ['inputText', 'Address Line 3', addressDetails.propertyAddressSection.addressLine3]
      , ['inputText', 'Town or City', addressDetails.propertyAddressSection.TownOrCity]
      , ['inputText', 'County', addressDetails.propertyAddressSection.walesCounty]
      , ['inputText', 'Postcode/Zipcode', addressDetails.propertyAddressSection.walesPostcode]
      , ['inputText', 'Country', addressDetails.propertyAddressSection.country]);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', legislativeCountry.countryOptions.wales);
    await performAction('clickButton', 'Continue');
    await performValidations('address info is not null '
      , ['visibility', claimantType.claimantTypeOptions.wales.privateLandlord]
      , ['visibility', claimantType.claimantTypeOptions.wales.registeredCommunityLandlord]
      , ['visibility', claimantType.claimantTypeOptions.wales.mortgageLender]
      , ['visibility', claimantType.claimantTypeOptions.wales.other]);
    await performAction('clickRadioButton', claimantType.claimantTypeOptions.wales.registeredCommunityLandlord);
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
    await performActions('Case option selection'
      , ['select', 'Jurisdiction', createCase.caseOption.jurisdiction.possessions]
      , ['select', 'Case type', createCase.caseOption.caseType.civilPossessions]
      , ['select', 'Event', createCase.caseOption.event.makeAPossessionClaim]);
    await performAction('clickButton', 'Start');
    await performActions('Find Address based on postcode'
      , ['inputText', 'Enter a UK postcode', addressDetails.propertyAddressSection.englandPostcode]
      , ['clickButton', 'Find address']
      , ['select', 'Select an address', addressDetails.propertyAddressSection.addressIndex]
      , ['inputText', 'Address Line 2', addressDetails.propertyAddressSection.addressLine2]
      , ['inputText', 'Address Line 3', addressDetails.propertyAddressSection.addressLine3]
      , ['inputText', 'County', addressDetails.propertyAddressSection.englandCounty]);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', legislativeCountry.countryOptions.england);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', claimantType.claimantTypeOptions.england.privateLandlord);
    await performAction('clickButton', 'Continue');
    await performValidation('mainHeader', "You're not eligible for this online service");
    await performAction('clickButton', 'Close and return to case list');

  });

  test('Wales - Unsuccessful case creation journey due to claimant type not in scope of Release1', async () => {

    await performAction('clickButton', 'Create case');
    await performActions('Case option selection'
      , ['select', 'Jurisdiction', createCase.caseOption.jurisdiction.possessions]
      , ['select', 'Case type', createCase.caseOption.caseType.civilPossessions]
      , ['select', 'Event', createCase.caseOption.event.makeAPossessionClaim]);
    await performAction('clickButton', 'Start');
    await performActions('Find Address based on postcode'
      , ['inputText', 'Enter a UK postcode', addressDetails.propertyAddressSection.walesPostcode]
      , ['clickButton', 'Find address']
      , ['select', 'Select an address', addressDetails.propertyAddressSection.addressIndex]
      , ['inputText', 'Address Line 2', addressDetails.propertyAddressSection.addressLine2]
      , ['inputText', 'Address Line 3', addressDetails.propertyAddressSection.addressLine3]
      , ['inputText', 'County', addressDetails.propertyAddressSection.walesCounty]);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', legislativeCountry.countryOptions.wales);
    await performAction('clickButton', 'Continue');
    await performAction('clickRadioButton', claimantType.claimantTypeOptions.wales.privateLandlord);
    await performAction('clickButton', 'Continue');
    await performValidation('mainHeader',  "You're not eligible for this online service");
    await performAction('clickButton', 'Close and return to case list');
  });

});
