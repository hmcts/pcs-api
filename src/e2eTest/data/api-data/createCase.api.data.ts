export const createCaseApiData = {
  createCaseEventName: 'createPossessionClaim',
  createCasePayload: {
    feeAmount: '£404',
    propertyAddress: {
      AddressLine1: '2 Second Avenue',
      AddressLine2: 'Oxford Street',
      AddressLine3: '',
      PostTown: 'London',
      County: '',
      PostCode: 'W3 7RX',
      Country: 'United Kingdom'
    },
    legislativeCountry: 'England'
  },
  createCaseApiEndPoint: `/case-types/PCS${process.env.CASE_TYPE_SUFFIX ? '-' + process.env.CASE_TYPE_SUFFIX : ''}/cases`,
};