package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import java.util.Locale;

public final class TestSupportEnvironment {

    public static final String PREVIEW = "preview";
    public static final String DEV = "dev";
    public static final String AAT = "aat";
    public static final String SPRING_PROFILES_ACTIVE = "SPRING_PROFILES_ACTIVE";

    private TestSupportEnvironment() {
    }

    public static boolean isDev() {
        return isProfile(DEV);
    }

    public static boolean isPreview() {
        return isProfile(PREVIEW);
    }

    private static boolean isProfile(String name) {
        String value = System.getenv(SPRING_PROFILES_ACTIVE);
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.UK);
        return lower.contains(name);
    }

    public static boolean isNonProdTestSupportEnabled() {
        return isNonProdTestSupportEnabled(
            System.getenv("ENVIRONMENT"),
            System.getenv(SPRING_PROFILES_ACTIVE),
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
        return lower.contains(DEV) || lower.contains(PREVIEW) || lower.contains(AAT);
    }
}

