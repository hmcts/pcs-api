
export const caseData = {
  jurisdiction: 'Possessions',
  caseType: process.env.CHANGE_ID
    ? `Civil Possessions ${process.env.CHANGE_ID}`
    : 'Civil Possessions',
  event: 'Make a claim',
  postcode: 'W3 7RX',
  addressIndex: 1,
  addressLine2: 'address2',
  addressLine3: 'address3',
  county: 'London',
  applicantFirstName: 'AutomationTestUser',
  manualBuildingAndStreet: '123 Test Street',
  manualTownOrCity: 'ManualTown',
  manualPostcode: 'MT1 2AB',
  manualCountry: 'United Kingdom',
};
