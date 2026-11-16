package uk.gov.hmcts.reform.pcs.service;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.Components;
import com.launchdarkly.sdk.server.integrations.TestData;
import com.launchdarkly.sdk.server.LDClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    private static final String FLAG_KEY = "bulk-print-enabled";

    @Mock
    private LDClient ldClient;

    @Captor
    private ArgumentCaptor<LDContext> contextCaptor;

    @Test
    void shouldTargetUsersWithinTheirEnvironment() throws Exception {
        TestData data = TestData.dataSource();
        data.update(data.flag(FeatureFlag.CASE_BUNDLE_VIEW.key()).variationForAll(false)
            .ifMatch("key", LDValue.of("selected-idam-id"))
            .andMatch("environment", LDValue.of("prod")).thenReturn(true));
        try (LDClient client = new LDClient("test", new LDConfig.Builder()
            .dataSource(data).events(Components.noEvents()).build())) {
            var prod = new FeatureToggleService(client, "prod");
            assertThat(prod.isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, "selected-idam-id")).isTrue();
            assertThat(prod.isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, "other-idam-id")).isFalse();
            assertThat(new FeatureToggleService(client, "aat")
                .isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, "selected-idam-id")).isFalse();
            assertThat(prod.isEnabled(FeatureFlag.CASE_BUNDLE_VIEW)).isFalse();
        }
    }

    @Test
    void shouldHideBundlesWhenFlagUnavailableOrIdentityMissing() throws Exception {
        try (LDClient client = new LDClient("test", new LDConfig.Builder().offline(true).build())) {
            var service = new FeatureToggleService(client, "prod");
            assertThat(service.isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, "selected-idam-id")).isFalse();
            assertThat(service.isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, null)).isFalse();
            assertThat(service.isEnabledFor(FeatureFlag.CASE_BUNDLE_VIEW, " ")).isFalse();
        }
    }

    @Test
    void shouldReturnVariationValueFromLdClient() {
        FeatureToggleService underTest = new FeatureToggleService(ldClient, "aat");
        when(ldClient.boolVariation(eq(FLAG_KEY), any(LDContext.class), eq(false))).thenReturn(true);

        assertThat(underTest.isEnabled(FeatureFlag.BULK_PRINT)).isTrue();
    }

    @Test
    void shouldBuildContextWithServiceKeyAndEnvironment() {
        FeatureToggleService underTest = new FeatureToggleService(ldClient, "aat");
        when(ldClient.boolVariation(eq(FLAG_KEY), contextCaptor.capture(), eq(false))).thenReturn(true);

        underTest.isEnabled(FeatureFlag.BULK_PRINT);

        LDContext context = contextCaptor.getValue();
        assertThat(context.getKind().toString()).isEqualTo("user");
        assertThat(context.getKey()).isEqualTo("pcs-api");
        assertThat(context.isAnonymous()).isTrue();
        assertThat(context.getValue("environment").stringValue()).isEqualTo("aat");
    }

    @Test
    void shouldReturnFalseWhenFlagOff() {
        FeatureToggleService underTest = new FeatureToggleService(ldClient, "aat");
        when(ldClient.boolVariation(eq(FLAG_KEY), any(LDContext.class), eq(false))).thenReturn(false);

        assertThat(underTest.isEnabled(FeatureFlag.BULK_PRINT)).isFalse();
    }

    @Test
    void shouldPassFlagFailSafeDefaultToLdClient() {
        FeatureToggleService underTest = new FeatureToggleService(ldClient, "default");

        underTest.isEnabled(FeatureFlag.BULK_PRINT);

        // the per-flag fail-safe default is what LaunchDarkly serves when it cannot evaluate
        verify(ldClient).boolVariation(eq(FLAG_KEY), any(LDContext.class), eq(FeatureFlag.BULK_PRINT.defaultValue()));
    }
}
