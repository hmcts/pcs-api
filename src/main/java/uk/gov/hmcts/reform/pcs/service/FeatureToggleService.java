package uk.gov.hmcts.reform.pcs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.launchdarkly.FeatureToggleApi;

// All pcs-api flag keys live here; one method per flag.
@Service
public class FeatureToggleService {

    private static final String ACCESS_CODE_HASHING = "access-code-hashing-enabled";

    private final FeatureToggleApi featureToggleApi;
    private final boolean accessCodeHashingDefault;

    public FeatureToggleService(FeatureToggleApi featureToggleApi,
                                @Value("${access-code.hashing-default:false}") boolean accessCodeHashingDefault) {
        this.featureToggleApi = featureToggleApi;
        this.accessCodeHashingDefault = accessCodeHashingDefault;
    }

    public boolean isAccessCodeHashingEnabled() {
        // per-env default (prod true, non-prod false) used when LD has no value for this env
        return featureToggleApi.isFeatureEnabled(ACCESS_CODE_HASHING, accessCodeHashingDefault);
    }
}
