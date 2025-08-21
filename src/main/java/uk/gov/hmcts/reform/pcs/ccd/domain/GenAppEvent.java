package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GenAppEvent {

    private final String id;
    private final String label;
    private final List<GenAppState> applicableStates;
    private final GenAppState endState;
    private final List<String> applicableRoles;

    public boolean isApplicableFor(GenAppState counterClaimState) {
        return applicableStates.contains(counterClaimState);
    }

}
