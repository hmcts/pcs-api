package uk.gov.hmcts.reform.pcs.service;

/**
 * Registry of LaunchDarkly flags used by pcs-api.
 *
 * <p><b>Add a flag</b> — one line: {@code MY_FLAG("ld-flag-key", defaultValue)}. The default is the
 * fail-safe value served when LaunchDarkly cannot be evaluated (offline / flag missing / wrong type).
 *
 * <p><b>Retire a flag</b> once the feature is standard — delete the constant. The compiler then marks
 * every {@code isEnabled(THAT_FLAG)} call site; drop the conditional, keep the now-permanent branch,
 * and archive the flag in LaunchDarkly. Mark short-lived flags as "temporary" in LaunchDarkly so the
 * cleanup sweep picks them up.
 */
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
