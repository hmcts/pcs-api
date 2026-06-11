package uk.gov.hmcts.reform.pcs.service;

import com.launchdarkly.sdk.LDContext;
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

    private static final String FLAG_KEY = "access-code-hashing-enabled";

    @Mock
    private LDClient ldClient;

    @Captor
    private ArgumentCaptor<LDContext> contextCaptor;

    @Test
    void shouldReturnVariationValueFromLdClient() {
        FeatureToggleService underTest = new FeatureToggleService(ldClient, "aat");
        when(ldClient.boolVariation(eq(FLAG_KEY), any(LDContext.class), eq(false))).thenReturn(true);

        assertThat(underTest.isEnabled(FeatureFlag.ACCESS_CODE_HASHING)).isTrue();
    }

    @Test
    void shouldBuildContextWithServiceKeyAndEnvironment() {
        FeatureToggleService underTest = new FeatureToggleService(ldClient, "aat");
        when(ldClient.boolVariation(eq(FLAG_KEY), contextCaptor.capture(), eq(false))).thenReturn(true);

        underTest.isEnabled(FeatureFlag.ACCESS_CODE_HASHING);

        LDContext context = contextCaptor.getValue();
        assertThat(context.getKey()).isEqualTo("pcs-api");
        assertThat(context.getValue("environment").stringValue()).isEqualTo("aat");
    }

    @Test
    void shouldPassFlagFailSafeDefaultToLdClient() {
        FeatureToggleService underTest = new FeatureToggleService(ldClient, "default");

        underTest.isEnabled(FeatureFlag.ACCESS_CODE_HASHING);

        verify(ldClient).boolVariation(eq(FLAG_KEY), any(LDContext.class), eq(false));
    }
}
