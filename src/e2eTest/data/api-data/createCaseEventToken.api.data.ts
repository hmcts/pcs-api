export const createCaseEventTokenApiData = {
  createCaseEventTokenApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      'experimental': 'experimental',
      'Accept': '*/*',
    }
  }),
  createCaseEventTokenApiEndPoint: `/case-types/PCS${process.env.CASE_TYPE_SUFFIX ? '-' + process.env.CASE_TYPE_SUFFIX : ''}/event-triggers/createPossessionClaim`,
};
