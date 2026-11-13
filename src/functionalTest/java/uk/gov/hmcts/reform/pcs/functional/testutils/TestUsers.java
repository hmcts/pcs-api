package uk.gov.hmcts.reform.pcs.functional.testutils;

import java.util.Objects;
import java.util.EnumMap;
import java.util.Map;
import uk.gov.hmcts.reform.pcs.functional.config.TestConstants;

public final class TestUsers {

    public enum UserType {
        systemUser,
        citizenUser,
        solicitorUser,
        localAuthoritySolicitor1,
        localAuthoritySolicitor2,
        orgSolicitor1,
        orgSolicitor2,
        hearingCentreAdmin,
        hearingCentreTeamLeader,
        defendantCitizen1
    }

    private static final Map<UserType, String> TOKENS = new EnumMap<>(UserType.class);

    static {
        for (UserType userType : UserType.values()) {
            if (!configuredUsername(userType).isBlank()) {
                TOKENS.put(userType, PcsIdamTokenClient.generateToken(userType));
            }
        }
    }

    private TestUsers() {
    }

    public static String getUsername(UserType userType) {
        Objects.requireNonNull(userType, "User type must not be null");
        String username = configuredUsername(userType);
        if (username.isBlank()) {
            throw new IllegalArgumentException("No username configured for user: " + userType);
        }
        return username;
    }

    private static String configuredUsername(UserType userType) {
        return switch (userType) {
            case systemUser -> TestConstants.IDAM_SYSTEM_USERNAME;
            case citizenUser -> TestConstants.PCS_CITIZEN_USER;
            case solicitorUser -> TestConstants.PCS_SOLICITOR_USER;
            case localAuthoritySolicitor1 -> TestConstants.LOCAL_AUTHORITY_SOLICITOR_1_USERNAME;
            case localAuthoritySolicitor2 -> TestConstants.LOCAL_AUTHORITY_SOLICITOR_2_USERNAME;
            case orgSolicitor1 -> TestConstants.ORG_SOLICITOR_1_USERNAME;
            case orgSolicitor2 -> TestConstants.ORG_SOLICITOR_2_USERNAME;
            case hearingCentreAdmin -> TestConstants.HEARING_CENTRE_ADMIN_USERNAME;
            case hearingCentreTeamLeader -> TestConstants.HEARING_CENTRE_TEAM_LEADER_USERNAME;
            case defendantCitizen1 -> TestConstants.DEFENDANT_CITIZEN_1_USERNAME;
        };
    }

    public static String getToken(UserType userType) {
        getUsername(userType);
        String token = TOKENS.get(userType);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No token generated for user: " + userType);
        }
        return token;
    }

    public static boolean isCitizenUser(UserType userType) {
        Objects.requireNonNull(userType, "User type must not be null");
        return userType == UserType.citizenUser || userType == UserType.defendantCitizen1;
    }

}
