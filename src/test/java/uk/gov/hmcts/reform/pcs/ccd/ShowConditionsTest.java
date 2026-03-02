package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

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

    private enum TestEnum {
        RED,
        GREEN,
        BLUE
    }

}
