package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShowConditions {

    public static final String NEVER_SHOW = "[STATE]=\"NEVER_SHOW\"";

    public static String stateEquals(State state) {
        return "[STATE]=\"%s\"".formatted(state.name());
    }

    public static String stateNotEquals(State state) {
        return "[STATE]!=\"%s\"".formatted(state.name());
    }

}
