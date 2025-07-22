import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import {
  initializeExecutor,
  performAction,
  performActions,
  performValidation, performValidations
} from '@utils/controller';
import configData from "@config/test.config";
import {headers} from "@data/header.data";
import {radioButton} from "@data/common.data";
import {caseOption} from "@data/page-data/caseOptions.data";
import {addressDetails} from "@data/page-data/addressDetails.data";
import {claimantTypeOptions} from "@data/page-data/claimantType.data";
import {applicantDtails} from "@data/page-data/ApplicantDetails.data";


test.beforeEach(async ({ page }, testInfo) => {

  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl',configData.manageCasesBaseURL);
  await performAction('login', 'exuiUser');

  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
});

test.describe('[Create Case Flow With Address and Claimant Type]  @Master @nightly', async () => {

test('Dropdown Address Selection Flow - should create case sucessfully', async () => {

    await performAction('click','Create case');
    await performValidation('mainHeader', {expected: headers.createCase});
    await performActions('Case option selection'
      ,['select', 'Jurisdiction', caseOption.jurisdiction]
      ,['select', 'Case type', caseOption.caseType]
      ,['select', 'Event', caseOption.event]);
    await performAction('click','Start');
    await performValidation('mainHeader', {expected:headers.selectAddress});
    await performActions('Find Address based on postcode'
      ,['fill', 'Enter a UK postcode', addressDetails.englandPostcode]
      ,['click', 'Find address']
      ,['select', 'Select an address', addressDetails.addressIndex]
      ,['fill', 'Address Line 2', addressDetails.addressLine2]
      ,['fill', 'Address Line 3', addressDetails.addressLine3]
      ,['fill', 'County', addressDetails.englandCounty]);
    await performAction('click', 'Continue');
    //HDPI-1271 Scenario 1
    await performValidation('mainHeader', {expected: headers.claimantType});


    await performValidation('validateOptionList', 'Claimant type or which type of claimant are you?', { elementType: radioButton,options : claimantTypeOptions.england});
    //Scenario 3
    await performAction('click', 'Continue');
    await performValidation('errorMessage', {
        header: 'There is a problem',
        errorHasLink: 'An address is required',
      });
    //HDPI-1271 Scenario 2
    await performAction('click','Claimant type or which type of claimant are you?',claimantTypeOptions.england.registeredProviderForSocialHousing);
    //or
    await performAction('click', 'Continue');

    await performValidation('mainHeader', {expected: headers.applicantDetails});
    await performAction('click', 'Submit');

    await performValidation('errorMessage', {
    header: 'There is a problem',
    errorHasLink: "Applicant's first name is required",
    });

    await performAction('fill', "Applicant's forename", applicantDtails.applicantFirstName);

    await performAction('click', 'Submit');
    await performValidation("bannerAlert", {message: "Case #.* has been created."});
    await performValidation('formLabelValue', "Applicant's forename", {value:'AutomationTestUser'});
    await performAction('clickTab', 'Property Details');
    await performValidation('formLabelValue', 'Building and Street');
    await performValidation('formLabelValue', 'Address Line 2');
    await performValidation('formLabelValue', 'Town or City');
    await performValidation('formLabelValue', 'Postcode/Zipcode');
    await performValidation('formLabelValue', 'Country');
    });
test('Manual Address Input Flow - should create case sucessfully', async () => {
    await performAction('click', 'Create case');
    await performActions('Case option selection'
      ,['select', 'Jurisdiction', caseData.jurisdiction]
      ,['select', 'Case type', caseData.caseType]
      ,['select', 'Event', caseData.event]);
    await performAction('click', 'Start');
    await performActions('Enter Address Manually'
      ,['click', "I can't enter a UK postcode"]
      ,['fill', 'Building and Street', caseData.BuildingAndStreet]
      ,['fill', 'Address Line 2', caseData.addressLine2]
      ,['fill', 'Address Line 3', caseData.addressLine3]
      ,['fill', 'Town or City', caseData.TownOrCity]
      ,['fill', 'County', caseData.walesCounty]
      ,['fill', 'Postcode/Zipcode', caseData.walesPostcode]
      ,['fill', 'Country', caseData.country]);
    await performAction('click','Continue');

    //Scenario 3
    await performAction('click', 'Continue');
    await performValidation('errorMessage', {
      header: 'There is a problem',
      errorHasLink: 'An address is required',
    });

    //HDPI-1271 Scenario 2
    await performAction('click',claimantType,claimantTypeOptions.wales.registeredCommunityLandlord);
    //or
    await performAction('click',claimantTypeOptions.wales.registeredCommunityLandlord);
    await performAction('click', 'Continue');

    await performAction('fill', "Applicant's forename", caseData.applicantFirstName);
    await performAction('click', 'Submit');
    await performValidation("bannerAlert", {message: "Case #.* has been created."});
    await performValidation('formLabelValue', "Applicant's forename", {value:caseData.applicantFirstName});
    await performAction('clickTab', 'Property Details');
    await performValidation('formLabelValue', 'Building and Street', {value:caseData.BuildingAndStreet});
    await performValidation('formLabelValue', 'Address Line 2', {value:caseData.addressLine2});
    await performValidation('formLabelValue', 'Town or City', {value:caseData.TownOrCity});
    await performValidation('formLabelValue', 'Postcode/Zipcode', {value:caseData.walesPostcode});
    await performValidation('formLabelValue', 'Country', {value:caseData.country});
  });

test('Unsucessful case creation journey due to claimant type not in scope of Release1 for England', async () => {

      await performAction('click','Create case');
      await performValidation('mainHeader', {expected: headers.createCase});
      await performActions('Case option selection'
        ,['select', 'Jurisdiction', caseData.jurisdiction]
        ,['select', 'Case type', caseData.caseType]
        ,['select', 'Event', caseData.event]);
      await performAction('click','Start');
      await performValidation('mainHeader', headers.selectAddress);
      await performActions('Find Address based on postcode'
        ,['fill', 'Enter a UK postcode', caseData.englandPostcode]
        ,['click', 'Find address']
        ,['select', 'Select an address', caseData.addressIndex]
        ,['fill', 'Address Line 2', caseData.addressLine2]
        ,['fill', 'Address Line 3', caseData.addressLine3]
        ,['fill', 'County', caseData.englandCounty]);
      await performAction('click', 'Continue');
      //HDPI-1271 Scenario 1
      await performValidation('mainHeader', {expected: headers.claimantType});

      //this can be converted as group validation
      await performValidation('visibility', claimantTypeOptions.england.registeredProviderForSocialHousing, { visible: true });
      await performValidation('visibility', claimantTypeOptions.england.mortgageProviderOrLender, { visible: true });
      await performValidation('visibility', claimantTypeOptions.england.privateLandlord, { visible: true });
      await performValidation('visibility', claimantTypeOptions.england.other, { visible: true });
      //OR
      await performValidations('Claimnat Type Options validation'
        ,['visibility', claimantTypeOptions.england.registeredProviderForSocialHousing, { visible: true }]
        ,['visibility', claimantTypeOptions.england.mortgageProviderOrLender, { visible: true }]
        ,['visibility', claimantTypeOptions.england.privateLandlord, { visible: true }]
        ,['visibility', claimantTypeOptions.england.other, { visible: true }],)
      //or
      await performValidation('validateOptionList', claimantType, { options : claimantTypeOptions.england});

      await performAction('click',claimantTypeOptions.england.privateLandlord);
      await performAction('click', 'Continue');
      await performValidation('mainHeader', {expected: "You're not eligible for this online service"});
      await performAction('click', 'Close and return to case list');


    });
test('Unsucessful case creation journey due to claimant type not in scope of Release1 for Wales', async () => {

      await performAction('click','Create case');
      await performValidation('mainHeader', {expected: headers.createCase});
      await performActions('Case option selection'
        ,['select', 'Jurisdiction', caseData.jurisdiction]
        ,['select', 'Case type', caseData.caseType]
        ,['select', 'Event', caseData.event]);
      await performAction('click','Start');
      await performValidation('mainHeader', headers.selectAddress);
      await performActions('Find Address based on postcode'
        ,['fill', 'Enter a UK postcode', caseData.walesPostcode]
        ,['click', 'Find address']
        ,['select', 'Select an address', caseData.addressIndex]
        ,['fill', 'Address Line 2', caseData.addressLine2]
        ,['fill', 'Address Line 3', caseData.addressLine3]
        ,['fill', 'County', caseData.walesCounty]);
      await performAction('click', 'Continue');
      await performValidation('mainHeader', {expected: headers.claimantType});

      //this can be converted as group validation
      await performValidation('visibility', claimantTypeOptions.england.registeredProviderForSocialHousing, { visible: true });
      await performValidation('visibility', claimantTypeOptions.england.mortgageProviderOrLender, { visible: true });
      await performValidation('visibility', claimantTypeOptions.england.privateLandlord, { visible: true });
      await performValidation('visibility', claimantTypeOptions.england.other, { visible: true });
      //OR
      await performValidations('Claimnat Type Options validation'
        ,['visibility', claimantTypeOptions.england.registeredProviderForSocialHousing, { visible: true }]
        ,['visibility', claimantTypeOptions.england.mortgageProviderOrLender, { visible: true }]
        ,['visibility', claimantTypeOptions.england.privateLandlord, { visible: true }]
        ,['visibility', claimantTypeOptions.england.other, { visible: true }],)
      //or
      await performValidation('validateOptionList', claimantType, { options : claimantTypeOptions.england});

      await performAction('click',claimantTypeOptions.wales.privateLandlord);
      await performAction('click', 'Continue');
      await performValidation('mainHeader', {expected: "You're not eligible for this online service"});
      await performAction('click', 'Close and return to case list');

    });
test('Unsucessful case creation journey due to claim type not in scope of Release1 for England', async () => {

    await performAction('click','Create case');
    await performValidation('mainHeader', {expected: headers.createCase});
    await performActions('Case option selection'
      ,['select', 'Jurisdiction', caseData.jurisdiction]
      ,['select', 'Case type', caseData.caseType]
      ,['select', 'Event', caseData.event]);
    await performAction('click','Start');
    await performValidation('mainHeader', headers.selectAddress);
    await performActions('Find Address based on postcode'
      ,['fill', 'Enter a UK postcode', caseData.englandPostcode]
      ,['click', 'Find address']
      ,['select', 'Select an address', caseData.addressIndex]
      ,['fill', 'Address Line 2', caseData.addressLine2]
      ,['fill', 'Address Line 3', caseData.addressLine3]
      ,['fill', 'County', caseData.englandCounty]);
    await performAction('click', 'Continue');
    //HDPI-1271 Scenario 1
    await performValidation('mainHeader', {expected: headers.claimantType});

    //this can be converted as group validation
    await performValidation('visibility', claimantTypeOptions.england.registeredProviderForSocialHousing, { visible: true });
    await performValidation('visibility', claimantTypeOptions.england.mortgageProviderOrLender, { visible: true });
    await performValidation('visibility', claimantTypeOptions.england.privateLandlord, { visible: true });
    await performValidation('visibility', claimantTypeOptions.england.other, { visible: true });
    //OR
    await performValidations('Claimnat Type Options validation'
      ,['visibility', claimantTypeOptions.england.registeredProviderForSocialHousing, { visible: true }]
      ,['visibility', claimantTypeOptions.england.mortgageProviderOrLender, { visible: true }]
      ,['visibility', claimantTypeOptions.england.privateLandlord, { visible: true }]
      ,['visibility', claimantTypeOptions.england.other, { visible: true }],)
    //or
    await performValidation('validateOptionList', claimantType, { options : claimantTypeOptions.england});

    await performAction('click',claimantTypeOptions.england.privateLandlord);
    await performAction('click', 'Continue');
    await performValidation('mainHeader', {expected: "You're not eligible for this online service"});
    await performAction('click', 'Close and return to case list');


  });
test('Unsucessful case creation journey due to claim type not in scope of Release1 for Wales', async () => {

    await performAction('click','Create case');
    await performValidation('mainHeader', {expected: headers.createCase});
    await performActions('Case option selection'
      ,['select', 'Jurisdiction', caseData.jurisdiction]
      ,['select', 'Case type', caseData.caseType]
      ,['select', 'Event', caseData.event]);
    await performAction('click','Start');
    await performValidation('mainHeader', headers.selectAddress);
    await performActions('Find Address based on postcode'
      ,['fill', 'Enter a UK postcode', caseData.walesPostcode]
      ,['click', 'Find address']
      ,['select', 'Select an address', caseData.addressIndex]
      ,['fill', 'Address Line 2', caseData.addressLine2]
      ,['fill', 'Address Line 3', caseData.addressLine3]
      ,['fill', 'County', caseData.walesCounty]);
    await performAction('click', 'Continue');
    await performValidation('mainHeader', {expected: headers.claimantType});

    //this can be converted as group validation
    await performValidation('visibility', claimantTypeOptions.england.registeredProviderForSocialHousing, { visible: true });
    await performValidation('visibility', claimantTypeOptions.england.mortgageProviderOrLender, { visible: true });
    await performValidation('visibility', claimantTypeOptions.england.privateLandlord, { visible: true });
    await performValidation('visibility', claimantTypeOptions.england.other, { visible: true });
    //OR
    await performValidations('Claimnat Type Options validation'
      ,['visibility', claimantTypeOptions.england.registeredProviderForSocialHousing, { visible: true }]
      ,['visibility', claimantTypeOptions.england.mortgageProviderOrLender, { visible: true }]
      ,['visibility', claimantTypeOptions.england.privateLandlord, { visible: true }]
      ,['visibility', claimantTypeOptions.england.other, { visible: true }],)
    //or
    await performValidation('validateOptionList', claimantType, { options : claimantTypeOptions.england});

    await performAction('click',claimantTypeOptions.wales.privateLandlord);
    await performAction('click', 'Continue');
    await performValidation('mainHeader', {expected: "You're not eligible for this online service"});
    await performAction('click', 'Close and return to case list');

  });
});
