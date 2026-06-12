package uk.gov.hmcts.reform.pcs.service;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeatureToggleService {

    private static final String SERVICE_KEY = "pcs-api";

    private final LDClient ldClient;
    private final String environment;

    public FeatureToggleService(LDClient ldClient,
                                @Value("${launchdarkly.env:default}") String environment) {
        this.ldClient = ldClient;
        this.environment = environment;
    }

    public boolean isEnabled(FeatureFlag flag) {
        return ldClient.boolVariation(flag.key(), context(), flag.defaultValue());
    }

    private LDContext context() {
        return LDContext.builder(SERVICE_KEY)
            .set("environment", environment)
            .build();
    }
}
