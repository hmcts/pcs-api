export class TestConfig {
  public static readonly manageCasesBaseURL: string =
    process.env.MANAGE_CASE_BASE_URL || "http://localhost:3000";
}

export default TestConfig;
