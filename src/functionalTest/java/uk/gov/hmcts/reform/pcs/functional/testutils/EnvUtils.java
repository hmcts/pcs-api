package uk.gov.hmcts.reform.pcs.functional.testutils;

public class EnvUtils {

    public static String getEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    /**
     * Gets environment variable with a default value if not set.
     * 
     * @param name Environment variable name
     * @param defaultValue Default value if variable is not set or blank
     * @return Environment variable value or default value
     */
    public static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
