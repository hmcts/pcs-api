package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppEvent;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.GenAppState.ACCEPTED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.GenAppState.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.GenAppState.ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.GenAppState.REJECTED;

@Service
public class GenAppEventService {

    private final List<GenAppEvent> events;

    public GenAppEventService() {
        this.events = configureEvents();
    }

    public GenAppEvent getEventByLabel(String label) {
        return events.stream()
            .filter(event -> event.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No event for label " + label));
    }

    public List<GenAppEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    private List<GenAppEvent> configureEvents() {
        return List.of(
            GenAppEvent.builder()
                .id("CREATE")
                .label("Create general application")  // Pseudo claim event just for history purposes
                .applicableRoles(List.of())
                .applicableStates(List.of())
                .build(),

            GenAppEvent.builder()
                .id("MAKE_PAYMENT")
                .label("Make payment")
                .applicableRoles(List.of(PCS_CASE_WORKER.getRole()))
                .applicableStates(List.of(AWAITING_SUBMISSION_TO_HMCTS))
                .endState(ISSUED)
                .build(),

            GenAppEvent.builder()
                .id("ACCEPT")
                .label("Accept general application")
                .applicableRoles(List.of(JUDGE.getRole()))
                .applicableStates(List.of(ISSUED))
                .endState(ACCEPTED)
                .build(),

            GenAppEvent.builder()
                .id("REJECT")
                .label("Reject general application")
                .applicableRoles(List.of(JUDGE.getRole()))
                .applicableStates(List.of(ISSUED))
                .endState(REJECTED)
                .build()

        );
    }

}
