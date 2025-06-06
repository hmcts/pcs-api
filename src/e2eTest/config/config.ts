export class Config {
  public static readonly manageCasesBaseURL: string =
    process.env.TEST_E2E_URL_EXUI || "http://localhost:3000";

  public static readonly e2e = {
    "testUrl": 'http://localhost:3209',
    "idamUrl": "https://idam-api.aat.platform.hmcts.net",
    "idamTestingSupportUrl": "https://idam-testing-support-api.aat.platform.hmcts.net",
    "loginEndpoint": "o/token",
    "grantType": "password",
    "scope": "profile openid roles",
    "clientId": "pcs-api",
    roles: 'caseworker-pcs',
  };
}

export default Config;
