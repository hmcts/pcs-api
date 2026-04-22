package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import java.util.Locale;

public final class TestSupportEnvironment {

    private TestSupportEnvironment() {
    }

    public static boolean isNonProdTestSupportEnabled() {
        return isNonProdTestSupportEnabled(
            System.getenv("ENVIRONMENT"),
            System.getenv("SPRING_PROFILES_ACTIVE"),
            System.getenv("ENABLE_TESTING_SUPPORT")
        );
    }

    static boolean isNonProdTestSupportEnabled(
        String environment,
        String springProfilesActive,
        String enableTestingSupport
    ) {
        return isStubEnvironment(environment)
            || isStubEnvironment(springProfilesActive)
            || "true".equalsIgnoreCase(enableTestingSupport);
    }

    private static boolean isStubEnvironment(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.UK);
        return lower.contains("dev") || lower.contains("preview") || lower.contains("aat");
    }

}

