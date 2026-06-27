package uk.gov.hmcts.reform.pcs.config;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LaunchDarklyConfigurationTest {

    private final LaunchDarklyConfiguration underTest = new LaunchDarklyConfiguration();

    @Test
    void shouldCreateOfflineClientWhenSdkKeyIsBlank() throws Exception {
        try (LDClient client = underTest.ldClient("", false, new String[0])) {
            // an offline client reports as initialised and never reaches the network
            assertThat(client.isInitialized()).isTrue();

            // offline => every evaluation returns the fail-safe default that was passed in
            LDContext context = LDContext.create("test");
            assertThat(client.boolVariation("any-flag", context, true)).isTrue();
            assertThat(client.boolVariation("any-flag", context, false)).isFalse();
        }
    }
}
