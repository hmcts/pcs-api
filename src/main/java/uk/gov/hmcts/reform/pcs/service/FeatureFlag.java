package uk.gov.hmcts.reform.pcs.service;

// LaunchDarkly flags; the boolean is the fail-safe default served when LD cannot be evaluated.
public enum FeatureFlag {

    BULK_PRINT("bulk-print-enabled", false),
    CASEWORKER_EVENTS("caseworker-events-enabled", false),
    CUI_RESPOND_TO_CLAIM_LR("cui-respond-to-claim-lr-enabled", false),
    RELEASE_1_DOT_2("release-1.2-enabled", false);

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
