export const enforceOrderEventTokenApiData = {
  submitCaseEventTokenApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      'experimental': 'experimental',
      'Accept': '*/*',
    }
  }),
  enforceEventTokenApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/event-triggers/enforceTheOrder`,
};