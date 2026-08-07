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
    // PoC branch: QKLHPMU org member holding the group access capacities, so the suite
    // exercises capacity-driven case creation. Restore the automation account + env uid
    // before merging towards master.
    public static final String PCS_SOLICITOR_USER = "pcs-solicitor-user001@test.com";
    public static final String PCS_SOLICITOR_AUTOMATION_IDAM_UID = "137eb5b3-acb1-4846-9ea3-b1f38b984f19";
    public static final String GENERIC_PASSWORD = getEnv("IDAM_PCS_USER_PASSWORD");
}
