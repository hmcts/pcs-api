package uk.gov.hmcts.reform.pcs.service;

public enum FeatureFlag {

    BULK_PRINT("bulk-print-enabled");

    private final String key;

    FeatureFlag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public boolean defaultValue() {
        return false;
    }
}
