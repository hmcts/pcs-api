package uk.gov.hmcts.reform.pcs.launchdarkly;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

// Wraps the LD SDK; typed flag methods live in FeatureToggleService.
@Slf4j
@Service
public class FeatureToggleApi {

    private static final String SERVICE_KEY = "pcs-api";

    private final LDClient ldClient;
    private final String environment;

    public FeatureToggleApi(LDClient ldClient, @Value("${launchdarkly.env:default}") String environment) {
        this.ldClient = ldClient;
        this.environment = environment;
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public boolean isFeatureEnabled(String feature, boolean defaultValue) {
        return ldClient.boolVariation(feature, createContext(), defaultValue);
    }

    private LDContext createContext() {
        // environment drives per-env targeting in LD
        return LDContext.builder(SERVICE_KEY)
            .set("environment", environment)
            .build();
    }

    private void close() {
        try {
            ldClient.close();
        } catch (IOException e) {
            log.error("Error closing the LaunchDarkly client", e);
        }
    }
}
