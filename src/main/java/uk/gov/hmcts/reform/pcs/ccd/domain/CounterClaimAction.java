package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;

import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimState.*;

// TODO: Better naming?
@Getter
public enum CounterClaimAction implements HasLabel {

    CREATE("Create counterclaim", List.of(), null, List.of()), // Pseudo action for use in history
    MAKE_PAYMENT("Make payment", List.of(AWAITING_SUBMISSION_TO_HMCTS), CLAIM_ISSUED, List.of(PCS_CASE_WORKER.getRole())),
    ADD_NOTE("Add note", List.of(CLAIM_ISSUED), null, List.of(PCS_CASE_WORKER.getRole())),
    REQUEST_BREATHING_SPACE("Request breathing space", List.of(CLAIM_ISSUED), null, List.of(PCS_CASE_WORKER.getRole())),
    REJECT("Reject counterclaim", List.of(CLAIM_ISSUED), CLAIM_REJECTED, List.of(JUDGE.getRole())),
    SETTLE("Settle counterclaim", List.of(CLAIM_ISSUED), CLAIM_RESOLVED, List.of(PCS_CASE_WORKER.getRole())),
    WITHDRAW("Withdraw counterclaim", List.of(AWAITING_SUBMISSION_TO_HMCTS, CLAIM_ISSUED), CLAIM_WITHDRAWN, List.of(PCS_CASE_WORKER.getRole()));

    private final String label;
    private final List<CounterClaimState> applicableStates;
    private final CounterClaimState endState;
    private final List<String> applicableRoles;

    CounterClaimAction(String label, List<CounterClaimState> applicableStates) {
        this.label = label;
        this.applicableStates = applicableStates;
        this.endState = null;
        this.applicableRoles = List.of();
    }

    CounterClaimAction(String label, List<CounterClaimState> applicableStates, CounterClaimState endState) {
        this.label = label;
        this.applicableStates = applicableStates;
        this.endState = endState;
        this.applicableRoles = List.of();;
    }

    CounterClaimAction(String label, List<CounterClaimState> applicableStates, CounterClaimState endState, List<String> applicableRoles) {
        this.label = label;
        this.applicableStates = applicableStates;
        this.endState = endState;
        this.applicableRoles = applicableRoles;
    }

    public static CounterClaimAction fromLabel(String label) {
        if (label == null) {
            return null;
        }

        return Arrays.stream(values())
            .filter(value -> label.equals(value.getLabel()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown label " + label));
    }

    public boolean isApplicableFor(CounterClaimState counterClaimState) {
        return applicableStates.contains(counterClaimState);
    }

}
