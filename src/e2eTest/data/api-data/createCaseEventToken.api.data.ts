export const createCaseEventTokenApiData = {
  createCaseApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      'experimental': 'experimental',
      'Accept': '*/*',
    }
  }),
  createCaseEventTokenApiEndPoint: `/case-types/PCS${process.env.CHANGE_ID ? '-' + process.env.CHANGE_ID : ''}/event-triggers/createPossessionClaim`,
}
