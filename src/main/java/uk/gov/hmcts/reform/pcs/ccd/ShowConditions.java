package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShowConditions {

    public static final String NEVER_SHOW = "[STATE]=\"NEVER_SHOW\"";

    public static String isYes(String fieldName) {
        return "%s=\"Yes\"".formatted(fieldName);
    }

    public static String isNo(String fieldName) {
        return "%s=\"No\"".formatted(fieldName);
    }

    public static String stateIs(State state) {
        return "[STATE]=\"%s\"".formatted(state.name());
    }

    public static String stateIsNot(State state) {
        return "[STATE]!=\"%s\"".formatted(state.name());
    }

}
