package uk.gov.hmcts.reform.pcs.functional.config;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.functional.testutils.EnvUtils.getEnv;

public class TestConstants {
    public static final String PCS_API = "pcs_api";
    public static final String PCS_FRONTEND = "pcs_frontend";
    public static final String CIVIL_SERVICE = "civil_service";
    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String EXPIRED_S2S_TOKEN = getEnv("S2S_EXPIRED_TOKEN");
    public static final String EXPIRED_IDAM_TOKEN = getEnv("IDAM_EXPIRED_USER_TOKEN");

    public static final String PCS_SOLICITOR_USER = "pcs-solicitor-automation@test.com";
    public static final String IDAM_SYSTEM_USERNAME = getEnv("IDAM_SYSTEM_USERNAME");
    public static final String IDAM_SYSTEM_PASSWORD = getEnv("IDAM_SYSTEM_USER_PASSWORD");
    public static final String PCS_CITIZEN_USER = "pcs-citizen-" + UUID.randomUUID() + "@test.com";
    
    public static final String LOCAL_AUTHORITY_SOLICITOR_1_USERNAME = "pcs.local.auth1@hmcts.net";
    public static final String LOCAL_AUTHORITY_SOLICITOR_2_USERNAME = "";
    public static final String ORG_SOLICITOR_1_USERNAME = "pcs.solicitor.org1@hmcts.net";
    public static final String ORG_SOLICITOR_2_USERNAME = "";
    public static final String HEARING_CENTRE_ADMIN_USERNAME = "";
    public static final String HEARING_CENTRE_TEAM_LEADER_USERNAME = "";
    public static final String DEFENDANT_CITIZEN_1_USERNAME = "PCS_citizen@gmail.com";
    public static final String PCS_SOLICITOR_AUTOMATION_IDAM_UID = getEnv("PCS_SOLICITOR_AUTOMATION_UID");
    public static final String GENERIC_PASSWORD = getEnv("IDAM_PCS_USER_PASSWORD");

}
