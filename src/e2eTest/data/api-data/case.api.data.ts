export const caseApiData = {
  microservice: 'ccd_data',
  eventName:'createPossessionClaim',
  createCasePayload: {
    propertyAddress: {
      AddressLine1: '48 Steer Road',
      AddressLine2: '',
      AddressLine3: '',
      PostTown: 'Swanage',
      County: '',
      PostCode: 'BH19 2RX',
      Country: 'United Kingdom'
    },
    applicantForename: 'Preset value'
  },
  createCaseApiInstance:{
    baseURL: `https://ccd-data-store-api-pcs-api-pr-${process.env.CHANGE_ID}.preview.platform.hmcts.net`,
    headers: {
      Authorization: `Bearer ${process.env.IDAM_AUTH_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      'experimental': 'experimental',
      'Accept': '*/*',
    }
  },
  eventTokenApiEndPoint: `/case-types/PCS-${process.env.CHANGE_ID}/event-triggers/createPossessionClaim`,
  createCaseApiEndPoint: `/case-types/PCS-${process.env.CHANGE_ID}/cases`
}
