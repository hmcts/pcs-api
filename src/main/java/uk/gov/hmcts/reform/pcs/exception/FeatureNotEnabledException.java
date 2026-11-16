package uk.gov.hmcts.reform.pcs.exception;

import lombok.Getter;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

/**
 * Thrown when a request reaches functionality that is switched off by a LaunchDarkly feature flag.
 */
@Getter
public class FeatureNotEnabledException extends RuntimeException {

    private final transient FeatureFlag flag;

    public FeatureNotEnabledException(FeatureFlag flag) {
        super("Feature flag " + flag.key() + " is not enabled");
        this.flag = flag;
    }
}
