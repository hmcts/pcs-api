package uk.gov.hmcts.reform.pcs.functional.config;

import static uk.gov.hmcts.reform.pcs.functional.testutils.EnvUtils.getEnv;

public class TestConstants {
    public static final String PCS_API = "pcs_api";
    public static final String PCS_FRONTEND = "pcs_frontend";
    public static final String CIVIL_SERVICE = "civil_service";
    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String EXPIRED_S2S_TOKEN = getEnv("S2S_EXPIRED_TOKEN");
    public static final String EXPIRED_IDAM_TOKEN = getEnv("IDAM_EXPIRED_USER_TOKEN");
}
