export class ConfigData {
  public static readonly manageCasesBaseURL: string =
    process.env.TEST_E2E_URL_EXUI || "http://localhost:3000";

  public static readonly iDam = {
    "idamUrl": "https://idam-api.aat.platform.hmcts.net",
    "idamTestingSupportUrl": "https://idam-testing-support-api.aat.platform.hmcts.net",
    "loginEndpoint": "o/token",
    "grantType": "password",
    "scope": "profile openid roles",
    "clientId": "pcs-api",
    "roles": ['caseworker-pcs','caseworker'],
  };
  public static readonly localHost = {
    env: 'localhost',
    username: process.env.TEST_USERNAME || '',
    password: process.env.TEST_PASSWORD || '',
  };
}

export default ConfigData;
