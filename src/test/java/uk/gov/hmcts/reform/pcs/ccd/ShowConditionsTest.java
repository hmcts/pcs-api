package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;

class ShowConditionsTest {

    @Test
    void shouldCreateShowConditionForStateEquals() {
        String showCondition = ShowConditions.stateEquals(State.AWAITING_SUBMISSION_TO_HMCTS);

        assertThat(showCondition).isEqualTo("[STATE]=\"AWAITING_SUBMISSION_TO_HMCTS\"");
    }

    @Test
    void shouldCreateShowConditionForStateNotEquals() {
        String showCondition = ShowConditions.stateNotEquals(State.AWAITING_SUBMISSION_TO_HMCTS);

        assertThat(showCondition).isEqualTo("[STATE]!=\"AWAITING_SUBMISSION_TO_HMCTS\"");
    }

    @Test
    void shouldCreateShowConditionForFieldEquals() {
        String fieldId = "testFieldId1";

        String showCondition = ShowConditions.fieldEquals(fieldId, TestEnum.GREEN);

        assertThat(showCondition).isEqualTo("testFieldId1=\"GREEN\"");
    }

    @Test
    void shouldCreateShowConditionForFieldContains() {
        String fieldId = "testFieldId1";

        String showCondition = ShowConditions.fieldContains(fieldId, TestEnum.BLUE);

        assertThat(showCondition).isEqualTo("testFieldId1CONTAINS\"BLUE\"");
    }

    @ParameterizedTest
    @MethodSource("joinWithAndScenarios")
    void shouldJoinShowConditionWithAnd(List<String> showConditionsToJoin, String expectedJoinedShowConditions) {
        String showCondition = ShowConditions.and(showConditionsToJoin.toArray(new String[0]));

        assertThat(showCondition).isEqualTo(expectedJoinedShowConditions);
    }

    private static Stream<Arguments> joinWithAndScenarios() {
        return Stream.of(
            // Show conditions to join, expected joined show condition
            argumentSet("no params", List.of(), ""),
            argumentSet("one param", List.of("a"), "a"),
            argumentSet("two params", List.of("a", "b"), "a AND b"),
            argumentSet("three params", List.of("a", "b", "c"), "a AND b AND c")
        );
    }

    @ParameterizedTest
    @MethodSource("featureFlagScenarios")
    void shouldCreateShowConditionForFeatureFlags(List<FeatureFlag> featureFlags,
                                                  String expectedShowCondition) {

        // When
        String actualShowCondition = ShowConditions.featureFlagsEnabled(featureFlags.toArray(new FeatureFlag[0]));

        // Then
        assertThat(actualShowCondition).isEqualTo(expectedShowCondition);
    }

    @ParameterizedTest
    @EnumSource(value = FeatureFlag.class, names = {"RELEASE_1_DOT_2", "CASEWORKER_EVENTS"}, mode = EXCLUDE)
    void shouldThrowExceptionForFeatureFlagWithNoCcdField(FeatureFlag featureFlag) {
        // When
        Throwable throwable = catchThrowable(() -> ShowConditions.featureFlagsEnabled(featureFlag));

        // Then
        assertThat(throwable)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Flag %s does not have a CCD field yet", featureFlag.name());
    }

    private static Stream<Arguments> featureFlagScenarios() {
        return Stream.of(
            // Feature flag(s), expected show condition
            arguments(List.of(),
                      ""),
            arguments(List.of(RELEASE_1_DOT_2),
                      "featureFlags.release1dot2Enabled=\"YES\""),
            arguments(List.of(CASEWORKER_EVENTS),
                      "featureFlags.caseWorkerEventsEnabled=\"YES\""),
            arguments(List.of(RELEASE_1_DOT_2, CASEWORKER_EVENTS),
                      "featureFlags.release1dot2Enabled=\"YES\" AND featureFlags.caseWorkerEventsEnabled=\"YES\"")
        );
    }

    private enum TestEnum {
        RED,
        GREEN,
        BLUE
    }

}
