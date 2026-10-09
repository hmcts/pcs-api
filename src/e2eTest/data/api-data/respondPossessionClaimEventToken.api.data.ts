export const respondPossessionClaimEventTokenApiData = {
  respondPossessionClaimApiInstance: (): Record<string, unknown> => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.CITIZEN_ACCESS_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      experimental: 'experimental',
      Accept: '*/*',
    },
  }),

  respondPossessionClaimApiEndPoint: (): string =>
    `/cases/${process.env.CASE_NUMBER}/event-triggers/respondPossessionClaim?ignore-warning=false`,
};

export const respondPossessionClaimSolicitorEventTokenApiData = {
  respondPossessionClaimSolicitorApiInstance: (): Record<string, unknown> => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.SOLICITOR_ACCESS_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      experimental: 'experimental',
      Accept: '*/*',
    },
  }),
  respondPossessionClaimSolicitorApiEndPoint: (): string =>
    `/cases/${process.env.CASE_NUMBER}/event-triggers/respondPossessionClaim`,
};
