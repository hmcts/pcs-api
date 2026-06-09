package uk.gov.hmcts.reform.pcs.launchdarkly;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureToggleApiTest {

    private static final String FEATURE = "access-code-hashing-enabled";
    private static final String ENVIRONMENT = "aat";

    @Mock
    private LDClient ldClient;

    @Captor
    private ArgumentCaptor<LDContext> contextCaptor;

    @Test
    void shouldReturnFlagValueFromLdClient() {
        FeatureToggleApi underTest = new FeatureToggleApi(ldClient, ENVIRONMENT);
        when(ldClient.boolVariation(eq(FEATURE), contextCaptor.capture(), eq(true))).thenReturn(false);

        boolean result = underTest.isFeatureEnabled(FEATURE, true);

        assertThat(result).isFalse();
    }

    @Test
    void shouldBuildContextWithServiceKeyAndEnvironment() {
        FeatureToggleApi underTest = new FeatureToggleApi(ldClient, ENVIRONMENT);
        when(ldClient.boolVariation(eq(FEATURE), contextCaptor.capture(), eq(false))).thenReturn(true);

        boolean result = underTest.isFeatureEnabled(FEATURE, false);

        assertThat(result).isTrue();
        LDContext context = contextCaptor.getValue();
        assertThat(context.getKey()).isEqualTo("pcs-api");
        assertThat(context.getValue("environment").stringValue()).isEqualTo(ENVIRONMENT);
    }

    @Test
    void shouldDefaultEnvironmentAttributeWhenConfigured() {
        FeatureToggleApi underTest = new FeatureToggleApi(ldClient, "default");
        when(ldClient.boolVariation(eq(FEATURE), contextCaptor.capture(), eq(true))).thenReturn(true);

        underTest.isFeatureEnabled(FEATURE, true);

        assertThat(contextCaptor.getValue().getValue("environment").stringValue()).isEqualTo("default");
    }
}
