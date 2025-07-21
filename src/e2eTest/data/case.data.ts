
export const caseData = {
  jurisdiction: 'Possessions',
  caseType: process.env.CHANGE_ID
    ? `Civil Possessions ${process.env.CHANGE_ID}`
    : 'Civil Possessions',
  event: 'Make a claim',
  englandPostcode: 'W3 7RX',
  walesPostcode: 'CF61 1ZH',
  addressIndex: 1,
  addressLine2: 'address2',
  addressLine3: 'address3',
  englandCounty: 'London',
  walesCounty: 'Cardiff',
  applicantFirstName: 'AutomationTestUser',
  BuildingAndStreet: '123 Test Street',
  TownOrCity: 'ManualTown',
  postcode: 'MT1 2AB',
  country: 'United Kingdom',
  };

export const claimantTypeOptions = {
  wales:
    {
      privateLandlord: 'Private landlord',
      registeredCommunityLandlord: 'Registered community landlord',
      mortgageProviderOrLender: 'Mortgage Provider or lender',
      other: 'Other',
    },
  england:
    {
      privateLandlord: 'Private landlord',
      registeredProviderForSocialHousing: 'Private registered provider of social housing',
      mortgageProviderOrLender: 'Mortgage Provider or lender',
      other: 'Other',
    }
};

export const claimantType = 'Claimant type';
