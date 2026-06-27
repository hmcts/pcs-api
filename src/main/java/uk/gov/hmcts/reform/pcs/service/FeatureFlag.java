package uk.gov.hmcts.reform.pcs.service;

// LaunchDarkly flags; the boolean is the fail-safe default served when LD cannot be evaluated.
public enum FeatureFlag {

    BULK_PRINT("bulk-print-enabled", false);

    private final String key;
    private final boolean defaultValue;

    FeatureFlag(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public boolean defaultValue() {
        return defaultValue;
    }
}
