package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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
            Arguments.argumentSet("no params", List.of(), ""),
            Arguments.argumentSet("one param", List.of("a"), "a"),
            Arguments.argumentSet("two params", List.of("a", "b"), "a AND b"),
            Arguments.argumentSet("three params", List.of("a", "b", "c"), "a AND b AND c")
        );
    }

    private enum TestEnum {
        RED,
        GREEN,
        BLUE
    }

}
