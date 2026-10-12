package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureFlags;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CUI_RESPOND_TO_CLAIM_LR;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_4;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.WALES_MAKE_A_CLAIM;

@ExtendWith(MockitoExtension.class)
class FeatureFlagViewTest {

    private static final Map<FeatureFlag, Function<FeatureFlags, VerticalYesNo>> FLAG_GETTERS = Map.of(
        CASEWORKER_EVENTS, FeatureFlags::getCaseWorkerEventsEnabled,
        RELEASE_1_DOT_2, FeatureFlags::getRelease1dot2Enabled,
        RELEASE_1_DOT_3, FeatureFlags::getRelease1dot3Enabled,
        RELEASE_1_DOT_4, FeatureFlags::getRelease1dot4Enabled,
        WALES_MAKE_A_CLAIM, FeatureFlags::getWalesMakeAClaimEnabled,
        CUI_RESPOND_TO_CLAIM_LR, FeatureFlags::getCuiRespondToClaimLrEnabled,
        ENFORCEMENT, FeatureFlags::getEnforcementEnabled
    );

    @Mock
    private FeatureToggleService featureToggleService;

    private FeatureFlagView underTest;

    @BeforeEach
    void setUp() {
        underTest = new FeatureFlagView(featureToggleService);
    }

    @ParameterizedTest
    @MethodSource("featureFlagScenarios")
    void shouldSetFeatureFlagInCaseData(FeatureFlag featureFlag,
                                        Function<FeatureFlags, VerticalYesNo> getter,
                                        boolean flagEnabled) {
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSetRelease1dot3FeatureFlagInCaseData(boolean flagEnabled) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        when(featureToggleService.isEnabled(isA(FeatureFlag.class))).thenReturn(false);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)).thenReturn(flagEnabled);

        // When
        underTest.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getFeatureFlags().getRelease1dot3Enabled())
            .isEqualTo(VerticalYesNo.from(flagEnabled));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSetRelease1dot4FeatureFlagInCaseData(boolean flagEnabled) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        when(featureToggleService.isEnabled(isA(FeatureFlag.class))).thenReturn(false);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(flagEnabled);

        // When
        underTest.setCaseFields(pcsCase);

        // Then
        assertThat(pcsCase.getFeatureFlags().getRelease1dot4Enabled())
            .isEqualTo(VerticalYesNo.from(flagEnabled));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSetWalesMakeAClaimFeatureFlagInCaseData(boolean flagEnabled) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        when(featureToggleService.isEnabled(any(FeatureFlag.class)))
            .thenAnswer(invocation -> invocation.getArgument(0) == featureFlag && flagEnabled);

        // When
        underTest.setCaseFields(pcsCase);

        // Then
        assertThat(getter.apply(pcsCase.getFeatureFlags())).isEqualTo(VerticalYesNo.from(flagEnabled));
    }

    private static Stream<Arguments> featureFlagScenarios() {
        return FLAG_GETTERS.entrySet().stream()
            .flatMap(entry -> Stream.of(true, false)
                .map(enabled -> argumentSet(
                    entry.getKey() + " enabled=" + enabled,
                    entry.getKey(), entry.getValue(), enabled
                )));
    }

}
