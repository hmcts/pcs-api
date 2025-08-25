package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimState;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimState.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimState.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimState.CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimState.CLAIM_REJECTED;

@Service
public class CounterClaimEventService {

    public static final String CLAIM_APPLICANT = "[CLAIM_APPLICANT]";
    public static final String CLAIM_RESPONDENT = "[CLAIM_RESPONDENT]";

    private final List<CounterClaimEvent> events;

    public CounterClaimEventService() {
        this.events = configureEvents();
    }

    public CounterClaimEvent getEventByLabel(String label) {
        return events.stream()
            .filter(event -> event.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No event for label " + label));
    }

    public List<CounterClaimEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    private List<CounterClaimEvent> configureEvents() {
        return List.of(
            CounterClaimEvent.builder()
                .id("CREATE")
                .label("Create counterclaim")  // Pseudo claim event just for history purposes
                .applicableRoles(List.of())
                .applicableStates(List.of())
                .build(),

            CounterClaimEvent.builder()
                .id("MAKE_PAYMENT")
                .label("Make payment")
                .applicableRoles(List.of(CLAIM_APPLICANT))
                .applicableStates(List.of(AWAITING_SUBMISSION_TO_HMCTS))
                .endState(CLAIM_ISSUED)
                .build(),

            CounterClaimEvent.builder()
                .id("ADD_NOTE")
                .label("Add note")
                .applicableRoles(List.of(PCS_CASE_WORKER.getRole()))
                .applicableStates(List.of(CLAIM_ISSUED))
                .build(),

            CounterClaimEvent.builder()
                .id("REQUEST_BREATHING_SPACE")
                .label("Request breathing space")
                .applicableRoles(List.of(CLAIM_RESPONDENT))
                .applicableStates(List.of(CLAIM_ISSUED))
                .endState(BREATHING_SPACE)
                .build(),

            CounterClaimEvent.builder()
                .id("REJECT")
                .label("Reject counterclaim")
                .applicableRoles(List.of(JUDGE.getRole()))
                .applicableStates(List.of(CLAIM_ISSUED))
                .endState(CLAIM_REJECTED)
                .build(),

            CounterClaimEvent.builder()
                .id("SETTLE")
                .label("Settle counterclaim")
                .applicableRoles(List.of(CLAIM_RESPONDENT))
                .applicableStates(List.of(CLAIM_ISSUED))
                .endState(CounterClaimState.CLAIM_RESOLVED)
                .build(),

            CounterClaimEvent.builder()
                .id("WITHDRAW")
                .label("Withdraw counterclaim")
                .applicableRoles(List.of(CLAIM_APPLICANT))
                .applicableStates(List.of(CLAIM_ISSUED))
                .endState(CounterClaimState.CLAIM_WITHDRAWN)
                .build()
        );
    }

}
