package uk.gov.hmcts.reform.pcs.config;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LaunchDarklyConfigurationTest {

    private final LaunchDarklyConfiguration underTest = new LaunchDarklyConfiguration();

    @Test
    void shouldRunOfflineWhenOfflineModeTrueEvenWithKeyPresent() throws Exception {
        // LAUNCHDARKLY_OFFLINE=true forces offline regardless of a configured key (used by local/cftlibTest)
        try (LDClient client = underTest.ldClient("sdk-key-present", true)) {
            assertThat(client.isInitialized()).isTrue();

            LDContext context = LDContext.create("test");
            assertThat(client.boolVariation("any-flag", context, true)).isTrue();
            assertThat(client.boolVariation("any-flag", context, false)).isFalse();
        }
    }

}
