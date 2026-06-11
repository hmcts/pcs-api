package uk.gov.hmcts.reform.pcs.service;

/**
 * Registry of pcs-api LaunchDarkly flags. One constant per flag; the key and its fail-safe default
 * live here and nowhere else. Add a flag with one line; remove one by deleting its constant (the
 * compiler then points at every {@code isEnabled} call site).
 */
public enum FeatureFlag {

    ACCESS_CODE_HASHING("access-code-hashing-enabled");

    private final String key;

    FeatureFlag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** Served when LaunchDarkly can't be evaluated (unreachable / missing flag). Off is the safe mode. */
    public boolean defaultValue() {
        return false;
    }
}
