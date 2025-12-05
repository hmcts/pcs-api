package uk.gov.hmcts.reform.pcs.functional.testUtils;

public class EnvUtils {

    private EnvUtils() {}

    public static String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
