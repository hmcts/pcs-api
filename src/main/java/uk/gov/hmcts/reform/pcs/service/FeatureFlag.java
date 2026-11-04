package uk.gov.hmcts.reform.pcs.service;

import org.apache.commons.lang3.BooleanUtils;

import java.util.Optional;

// LaunchDarkly flags; the boolean is the fail-safe default served when LD cannot be evaluated
// and there is no system property override
public enum FeatureFlag {

    BULK_PRINT("bulk-print-enabled", false),
    SDP_EXPORT("sdp-export-enabled", false),
    CASEWORKER_EVENTS("caseworker-events-enabled", true),
    CUI_RESPOND_TO_CLAIM_LR("cui-respond-to-claim-lr-enabled", true),
    RELEASE_1_DOT_2("release-1.2-enabled", true),
    RELEASE_1_DOT_3("release-1.3-enabled", true),
    RELEASE_1B("release-1b-enabled", false),
    RELEASE_1_DOT_4("release-1.4-enabled", false),
    WALES_MAKE_A_CLAIM("wales-make-a-claim-enabled", false),
    CASEWORKER_WA("caseworker-wa-enabled", true),
    MAKE_ORDER("make-order-enabled", false),
    ENFORCEMENT("enforcement-enabled", false);

    private final String key;
    private final boolean defaultValue;

    FeatureFlag(String key, boolean fallbackValue) {
        this.key = key;

        this.defaultValue = Optional.ofNullable(System.getProperty("feature." + key))
            .map(BooleanUtils::toBoolean)
            .orElse(fallbackValue);
    }

    public String key() {
        return key;
    }

    public boolean defaultValue() {
        return defaultValue;
    }
}
