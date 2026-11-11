package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.breathingspace;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.*;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@Component
@AllArgsConstructor
public class EnterBreathingSpace implements CCDConfig<PCSCase, State, UserRole> {

    private final EnforcementOrderService enforcementOrderService;

    @Override
    public void configureDecentralised(
        DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder
    ) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(
                    EventId.enterBreathingSpace.name(),
                    this::submit
                )
                .forState(State.CASE_ISSUED)
                .name("Enter Breathing Space")
                .grant(Permission.CRUD, CASEWORKER_ROLES)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary()
                .endButtonLabel("Submit");

        // Register the existing Breathing Space data-entry
        // pages here when their implementation is available.
    }

    private SubmitResponse<State> submit(
        EventPayload<PCSCase, State> eventPayload
    ) {
        enforcementOrderService.updateStatesForBreathingSpace(
            eventPayload.caseReference()
        );

        return SubmitResponse.<State>builder()
            .state(State.BREATHING_SPACE)
            .build();
    }


}
