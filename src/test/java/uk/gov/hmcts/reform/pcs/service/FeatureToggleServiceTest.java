package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private FeatureToggleService underTest;

    @Test
    void shouldReturnFalseWhenLdDisablesTheFlag() {
        when(featureToggleApi.isFeatureEnabled(FLAG_KEY, true)).thenReturn(false);

        assertThat(underTest.isAccessCodeHashingEnabled()).isFalse();
    }

    @Test
    void shouldFailSafeToHashingWithTrueDefault() {
        when(featureToggleApi.isFeatureEnabled(FLAG_KEY, true)).thenReturn(true);

        assertThat(underTest.isAccessCodeHashingEnabled()).isTrue();
        verify(featureToggleApi).isFeatureEnabled(FLAG_KEY, true);
    }
}
