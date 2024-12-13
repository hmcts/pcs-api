export class Config {
  public static readonly manageCasesBaseURL: string =
    process.env.TEST_E2E_URL_WEB || "http://localhost:3206";
}

export default Config;
