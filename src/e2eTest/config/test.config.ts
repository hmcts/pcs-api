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
    "roles": ['caseworker-pcs','caseworker'],
  };
}

export default TestConfig;
