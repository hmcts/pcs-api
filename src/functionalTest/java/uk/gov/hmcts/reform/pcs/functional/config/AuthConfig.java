package uk.gov.hmcts.reform.pcs.functional.config;

public class AuthConfig {
    public static final String CLIENT_ID = "pcs-api";
    public static final String GRANT_TYPE = "password";
    public static final String SCOPE = "profile openid roles";
    public static final String ENDPOINT = "/o/token";
}
