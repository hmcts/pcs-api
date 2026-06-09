package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.launchdarkly.FeatureToggleApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    private static final String FLAG_KEY = "access-code-hashing-enabled";

    @Mock
    private FeatureToggleApi featureToggleApi;

    @Test
    void shouldReturnLdValueOverTheDefault() {
        FeatureToggleService underTest = new FeatureToggleService(featureToggleApi, false);
        when(featureToggleApi.isFeatureEnabled(FLAG_KEY, false)).thenReturn(true);

        assertThat(underTest.isAccessCodeHashingEnabled()).isTrue();
    }

    @Test
    void shouldPassPerEnvDefaultToLd() {
        FeatureToggleService underTest = new FeatureToggleService(featureToggleApi, true);
        when(featureToggleApi.isFeatureEnabled(FLAG_KEY, true)).thenReturn(true);

        assertThat(underTest.isAccessCodeHashingEnabled()).isTrue();
        verify(featureToggleApi).isFeatureEnabled(FLAG_KEY, true);
    }
}
