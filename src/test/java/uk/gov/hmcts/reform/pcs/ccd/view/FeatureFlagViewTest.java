package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagViewTest {

    @Mock
    private FeatureToggleService featureToggleService;

    private FeatureFlagView underTest;

    @BeforeEach
    void setUp() {
        underTest = new FeatureFlagView(featureToggleService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSetCaseworkerFeatureFlagInCaseData(boolean flagEnabled) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        when(featureToggleService.isEnabled(isA(FeatureFlag.class))).thenReturn(false);
        when(featureToggleService.isEnabled(FeatureFlag.CASEWORKER_EVENTS)).thenReturn(flagEnabled);

        // When
        underTest.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getFeatureFlags().getCaseWorkerEventsEnabled())
            .isEqualTo(VerticalYesNo.from(flagEnabled));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSetRelease1dot2FeatureFlagInCaseData(boolean flagEnabled) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        when(featureToggleService.isEnabled(isA(FeatureFlag.class))).thenReturn(false);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(flagEnabled);

        // When
        underTest.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getFeatureFlags().getRelease1dot2Enabled())
            .isEqualTo(VerticalYesNo.from(flagEnabled));
    }

}
