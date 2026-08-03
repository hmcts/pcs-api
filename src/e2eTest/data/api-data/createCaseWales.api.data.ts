export const createCaseApiWalesData = {
  createCaseEventName: 'createPossessionClaim',
  createCasePayload: {
    feeAmount: '£404',
    propertyAddress: {
      AddressLine1: '2 Pentre Street',
      AddressLine2: 'Petre Avenue',
      AddressLine3: '',
      PostTown: 'Caerdydd',
      County: '',
      PostCode: 'CF11 6QX',
      Country: 'Deyrnas Unedig',
    },
    legislativeCountry: 'Wales',
  },
  createCaseApiEndPoint: `/case-types/PCS${
    process.env.PCS_API_CHANGE_ID ? '-' + process.env.PCS_API_CHANGE_ID : ''
  }/cases`,
};
