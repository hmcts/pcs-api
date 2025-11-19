export const submitCaseEventTokenApiData = {
  createCaseApiInstance: () => ({
    baseURL: `https://ccd-data-store-api-pcs-api-pr-${process.env.CHANGE_ID}.preview.platform.hmcts.net`,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      'experimental': 'experimental',
      'Accept': '*/*',
    }
  }),
  submitCaseEventTokenApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/event-triggers/resumePossessionClaim`
}
