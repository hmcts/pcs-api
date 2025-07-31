export class TestConfig {
  public static readonly manageCasesBaseURL: string =
    process.env.MANAGE_CASE_BASE_URL || "http://localhost:3000";

  public static readonly iDam = {
    "idamUrl": "https://idam-api.aat.platform.hmcts.net",
    "idamTestingSupportUrl": "https://idam-testing-support-api.aat.platform.hmcts.net",
    "loginEndpoint": "o/token",
    "grantType": "password",
    "scope": "profile openid roles",
    "clientId": "pcs-api",
    "roles": ['caseworker-pcs', 'caseworker'],
  };
  public static readonly authProvider = {
    url:process.env.IDAM_S2S_AUTH_URL,
    microservice: 'ccd_data',
    endPoint: '/testing-support/lease',
  }
  public static readonly ccdCase = {
    url: process.env.DATA_STORE_URL_BASE,
    //caseType: `PCS-${process.env.CHANGE_ID}`,
    caseType: 'PCS-394',
    eventName:'createTestApplication'
  }
}

export default TestConfig;
