package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.judicialDecisionRequired;

@Component
@AllArgsConstructor
public class JudicialDecisionRequired implements CCDConfig<PcsCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(judicialDecisionRequired.name(), this::submit)
            .forStateTransition(State.CaseIssued, State.JudicialReferral)
            .name("Judicial Decision Required")
            .grant(Permission.CRUD, UserRole.CIVIL_CASE_WORKER)
            .fields()
            .page("request-judicial-decision")
                .pageLabel("Confirm")
                .label("confirmation-info-judicial", "You are about to request a judicial decision")
            .done();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
    }

}
