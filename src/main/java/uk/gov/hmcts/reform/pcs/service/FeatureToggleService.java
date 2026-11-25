package uk.gov.hmcts.reform.pcs.service;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeatureToggleService {

    private static final String SERVICE_KEY = "pcs-api";

    private final LDClient ldClient;
    private final LDContext context;

    public FeatureToggleService(LDClient ldClient,
                                @Value("${launchdarkly.env:default}") String environment) {
        this.ldClient = ldClient;
        this.context = context(environment);
    }

    public boolean isEnabled(FeatureFlag flag) {
        return ldClient.boolVariation(flag.key(), context, flag.defaultValue());
    }

    // Anonymous service identity carrying the environment attribute for LD targeting; invariant, built once.
    private static LDContext context(String environment) {
        return LDContext.builder(SERVICE_KEY)
            .set("environment", environment)
            .anonymous(true)
            .build();
    }
}
