package uk.gov.hmcts.reform.pcs.service;

import com.launchdarkly.sdk.ContextKind;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeatureToggleService {

    private static final ContextKind SERVICE_KIND = ContextKind.of("service");
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

    /**
     * pcs-api has no per-request user, so flags evaluate against a single anonymous {@code service}-kind
     * context targeted by {@code environment} (deliberately distinct from pcs-frontend's per-user
     * context). The context is invariant for the bean's lifetime, so it is built once.
     */
    private static LDContext context(String environment) {
        return LDContext.builder(SERVICE_KIND, SERVICE_KEY)
            .set("environment", environment)
            .anonymous(true)
            .build();
    }
}
