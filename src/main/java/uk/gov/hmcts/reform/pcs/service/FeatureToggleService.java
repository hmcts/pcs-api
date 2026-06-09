package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.launchdarkly.FeatureToggleApi;

// All pcs-api flag keys live here; one method per flag.
@Service
@RequiredArgsConstructor
public class FeatureToggleService {

    private static final String ACCESS_CODE_HASHING = "access-code-hashing-enabled";

    private final FeatureToggleApi featureToggleApi;

    public boolean isAccessCodeHashingEnabled() {
        // default true: stays hashed if LD is unreachable, so prod is hashed unless explicitly toggled off
        return featureToggleApi.isFeatureEnabled(ACCESS_CODE_HASHING, true);
    }
}
