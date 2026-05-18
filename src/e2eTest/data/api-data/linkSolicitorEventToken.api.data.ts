

export const linkSolicitorTokenApiData = {
  linkSolicitorTokenApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.SOLICITOR_ACCESS_TOKEN}`,
      //Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      experimental: 'experimental',
      Accept: '*/*',
    },
  }),
  linkSolicitorTokenApiEndPoint: () => `/cases/${process.env.CASE_NUMBER}/event-triggers/makeAnApplication`,
};

