export const updateCaseStateEventTokenApiData = {
  updateCaseStateEventTokenApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN_USER}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      'experimental': 'experimental',
      'Accept': '*/*',
    }
  }),
  updateCaseStateEventTokenApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/event-triggers/changeCaseState`,
};
