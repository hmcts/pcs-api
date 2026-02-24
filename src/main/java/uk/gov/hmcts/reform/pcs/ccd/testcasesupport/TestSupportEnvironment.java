package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import java.util.Locale;

public final class TestSupportEnvironment {

    private TestSupportEnvironment() {
    }

    public static boolean isNonProdTestSupportEnabled() {
        return isStubEnvironment(System.getenv("ENVIRONMENT"))
            || isStubEnvironment(System.getenv("SPRING_PROFILES_ACTIVE"))
            || "true".equalsIgnoreCase(System.getenv("ENABLE_TESTING_SUPPORT"));
    }

    private static boolean isStubEnvironment(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.UK);
        return lower.contains("dev") || lower.contains("preview") || lower.contains("aat");
    }
}

