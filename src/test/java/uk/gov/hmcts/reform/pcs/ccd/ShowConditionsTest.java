package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CUI_RESPOND_TO_CLAIM_LR;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_4;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.WALES_MAKE_A_CLAIM;

class ShowConditionsTest {

    private static final Map<FeatureFlag, String> CCD_FIELDS_BY_FLAG = Map.of(
        RELEASE_1_DOT_2, "featureFlags.release1dot2Enabled",
        RELEASE_1_DOT_3, "featureFlags.release1dot3Enabled",
        RELEASE_1_DOT_4, "featureFlags.release1dot4Enabled",
        CASEWORKER_EVENTS, "featureFlags.caseWorkerEventsEnabled",
        WALES_MAKE_A_CLAIM, "featureFlags.walesMakeAClaimEnabled",
        CUI_RESPOND_TO_CLAIM_LR, "featureFlags.cuiRespondToClaimLrEnabled",
        ENFORCEMENT, "featureFlags.enforcementEnabled"
    );

    @Test
    void shouldCreateShowConditionForStateEquals() {
        assertThat(ShowConditions.stateEquals(State.AWAITING_SUBMISSION_TO_HMCTS))
            .isEqualTo("[STATE]=\"AWAITING_SUBMISSION_TO_HMCTS\"");
    }

    @Test
    void shouldCreateShowConditionForStateNotEquals() {
        assertThat(ShowConditions.stateNotEquals(State.AWAITING_SUBMISSION_TO_HMCTS))
            .isEqualTo("[STATE]!=\"AWAITING_SUBMISSION_TO_HMCTS\"");
    }

    @Test
    void shouldCreateShowConditionForFieldEquals() {
        assertThat(ShowConditions.fieldEquals("testFieldId1", TestEnum.GREEN))
            .isEqualTo("testFieldId1=\"GREEN\"");
    }

    @Test
    void shouldCreateShowConditionForFieldContains() {
        assertThat(ShowConditions.fieldContains("testFieldId1", TestEnum.BLUE))
            .isEqualTo("testFieldId1CONTAINS\"BLUE\"");
    }

    @ParameterizedTest
    @MethodSource("joinWithAndScenarios")
    void shouldJoinShowConditionsWithAnd(List<String> conditions, String expected) {
        assertThat(ShowConditions.and(conditions.toArray(String[]::new))).isEqualTo(expected);
    }

    private static Stream<Arguments> joinWithAndScenarios() {
        return Stream.of(
            argumentSet("no params", List.of(), ""),
            argumentSet("one param", List.of("a"), "a"),
            argumentSet("two params", List.of("a", "b"), "a AND b"),
            argumentSet("three params", List.of("a", "b", "c"), "a AND b AND c")
        );
    }

    @ParameterizedTest
    @MethodSource("joinWithOrScenarios")
    void shouldJoinShowConditionsWithOr(List<String> conditions, String expected) {
        assertThat(ShowConditions.or(conditions.toArray(String[]::new))).isEqualTo(expected);
    }

    private static Stream<Arguments> joinWithOrScenarios() {
        return Stream.of(
            argumentSet("no params", List.of(), ""),
            argumentSet("one param", List.of("a"), "a"),
            argumentSet("two params", List.of("a", "b"), "a OR b"),
            argumentSet("three params", List.of("a", "b", "c"), "a OR b OR c")
        );
    }

    @Test
    void shouldReturnEmptyShowConditionWhenNoFeatureFlags() {
        assertThat(ShowConditions.featureFlagsEnabled()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("supportedFeatureFlags")
    void shouldCreateShowConditionForSupportedFeatureFlag(FeatureFlag featureFlag) {
        assertThat(ShowConditions.featureFlagsEnabled(featureFlag))
            .isEqualTo(enabled(featureFlag));
    }

    @ParameterizedTest
    @MethodSource("unsupportedFeatureFlags")
    void shouldThrowExceptionForFeatureFlagWithNoCcdField(FeatureFlag featureFlag) {
        assertThatThrownBy(() -> ShowConditions.featureFlagsEnabled(featureFlag))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Flag %s does not have a CCD field yet", featureFlag.name());
    }

    @ParameterizedTest
    @MethodSource("featureFlagCombinations")
    void shouldJoinMultipleFeatureFlagsWithAnd(List<FeatureFlag> featureFlags, String expected) {
        assertThat(ShowConditions.featureFlagsEnabled(featureFlags.toArray(FeatureFlag[]::new)))
            .isEqualTo(expected);
    }

    private static Stream<FeatureFlag> supportedFeatureFlags() {
        return CCD_FIELDS_BY_FLAG.keySet().stream();
    }

    private static Stream<FeatureFlag> unsupportedFeatureFlags() {
        return Arrays.stream(FeatureFlag.values())
            .filter(flag -> !CCD_FIELDS_BY_FLAG.containsKey(flag));
    }

    private static Stream<Arguments> featureFlagCombinations() {
        return Stream.of(
            argumentSet("two flags",
                        List.of(RELEASE_1_DOT_2, CASEWORKER_EVENTS),
                        enabled(RELEASE_1_DOT_2) + " AND " + enabled(CASEWORKER_EVENTS)),
            argumentSet("two flags, order preserved",
                        List.of(CUI_RESPOND_TO_CLAIM_LR, RELEASE_1_DOT_3),
                        enabled(CUI_RESPOND_TO_CLAIM_LR) + " AND " + enabled(RELEASE_1_DOT_3)),
            argumentSet("three flags",
                        List.of(RELEASE_1_DOT_2, RELEASE_1_DOT_3, WALES_MAKE_A_CLAIM),
                        enabled(RELEASE_1_DOT_2) + " AND " + enabled(RELEASE_1_DOT_3)
                            + " AND " + enabled(WALES_MAKE_A_CLAIM))
        );
    }

    private static String enabled(FeatureFlag featureFlag) {
        return CCD_FIELDS_BY_FLAG.get(featureFlag) + "=\"YES\"";
    }

    private enum TestEnum {
        RED,
        GREEN,
        BLUE
    }

}
