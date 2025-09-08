export const accessTokenApiData = {
  idamUrl: 'https://idam-api.aat.platform.hmcts.net',
  idamTestingSupportUrl: 'https://idam-testing-support-api.aat.platform.hmcts.net',
  roles: ['caseworker-pcs', 'caseworker'],
  s2sUrl: 'http://rpe-service-auth-provider-aat.service.core-compute-aat.internal/testing-support/lease',
  accessTokenApiPayload: {
    username: process.env.IDAM_SYSTEM_USERNAME,
    password: process.env.IDAM_SYSTEM_USER_PASSWORD,
    client_id: 'pcs-api',
    client_secret: process.env.PCS_API_IDAM_SECRET,
    scope: 'profile openid roles',
    grant_type: 'password',
  }
}
