package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CounterClaimEvent {

    private final String id;
    private final String label;
    private final List<CounterClaimState> applicableStates;
    private final CounterClaimState endState;
    private final List<String> applicableRoles;

    public boolean isApplicableFor(CounterClaimState counterClaimState) {
        return applicableStates.contains(counterClaimState);
    }

}
