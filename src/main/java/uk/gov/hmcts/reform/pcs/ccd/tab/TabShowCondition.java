package uk.gov.hmcts.reform.pcs.ccd.tab;

import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public final class TabShowCondition {

    private TabShowCondition() {
    }

    public static String notShowForState(final State... states) {
        return Stream.of(states)
            .map(State::name)
            .collect(joining("\" AND [STATE]!=\"", "[STATE]!=\"", "\""));
    }

    public static String showForState(final State... states) {
        return Stream.of(states)
            .map(State::name)
            .collect(joining("\" OR [STATE]=\"", "[STATE]=\"", "\""));
    }
}
