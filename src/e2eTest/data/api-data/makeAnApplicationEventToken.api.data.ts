export const makeAnApplicationEventTokenApiData = {
  makeAnApplicationEventTokenApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.SOLICITOR_ACCESS_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      'experimental': 'experimental',
      'Accept': '*/*',
    }
  }),
  makeAnApplicationEventTokenApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/event-triggers/makeAnApplication`,
};