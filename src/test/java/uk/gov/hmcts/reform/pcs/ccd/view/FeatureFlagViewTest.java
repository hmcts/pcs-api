package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CUI_RESPOND_TO_CLAIM_LR;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.MAKE_ORDER;
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
        MAKE_ORDER, FeatureFlags::getMakeOrderEnabled,
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
    void shouldSetFeatureFlagInCaseData(FeatureFlag featureFlag, Function<FeatureFlags, VerticalYesNo> getter,
                                        boolean flagEnabled) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        when(featureToggleService.isEnabled(any(FeatureFlag.class)))
            .thenAnswer(invocation -> invocation.getArgument(0) == featureFlag && flagEnabled);

        // When
        underTest.setCaseFields(pcsCase);

        // Then
        assertThat(getter.apply(pcsCase.getFeatureFlags())).isEqualTo(VerticalYesNo.from(flagEnabled));
    }

    @ParameterizedTest
    @MethodSource("unsupportedFeatureFlags")
    void shouldNotQueryFeatureFlagWithNoCaseField(FeatureFlag featureFlag) {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase);

        // Then
        verify(featureToggleService, never()).isEnabled(featureFlag);
    }

    @Test
    void shouldHaveGetterForEveryFeatureFlagsField() {
        long fieldCount = Arrays.stream(FeatureFlags.class.getDeclaredFields())
            .filter(field -> !field.isSynthetic()).count();

        assertThat(FLAG_GETTERS).as("FLAG_GETTERS above must contain an entry for every field in FeatureFlags")
            .hasSize((int) fieldCount);
    }

    private static Stream<Arguments> featureFlagScenarios() {
        return FLAG_GETTERS.entrySet().stream()
            .flatMap(entry -> Stream.of(true, false)
                .map(enabled -> argumentSet(
                    entry.getKey() + " enabled=" + enabled,
                    entry.getKey(), entry.getValue(), enabled
                )));
    }

    private static Stream<FeatureFlag> unsupportedFeatureFlags() {
        return Arrays.stream(FeatureFlag.values())
            .filter(flag -> !FLAG_GETTERS.containsKey(flag));
    }

}
