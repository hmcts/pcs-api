package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.launchdarkly.FeatureToggleApi;

/**
 * Typed facade for pcs-api feature flags. Every flag-key string lives only here so
 * call sites never depend on raw LaunchDarkly keys. One method per flag.
 */
@Service
@RequiredArgsConstructor
public class FeatureToggleService {

    private static final String ACCESS_CODE_HASHING = "access-code-hashing-enabled";

    private final FeatureToggleApi featureToggleApi;

    public boolean isAccessCodeHashingEnabled() {
        // Fail safe to hashing when LD is unreachable. Prod runs offline (no SDK key) and must
        // always hash; only non-prod envs mount a key and toggle this off for testing.
        return featureToggleApi.isFeatureEnabled(ACCESS_CODE_HASHING, true);
    }
}
