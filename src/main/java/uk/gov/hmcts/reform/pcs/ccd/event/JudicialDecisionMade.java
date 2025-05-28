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

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.judicialDecisionMade;

@Component
@AllArgsConstructor
public class JudicialDecisionMade implements CCDConfig<PcsCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(judicialDecisionMade.name(), this::submit)
            .forStateTransition(State.JudicialReferral, State.CaseIssued)
            .name("Judicial Decision Made")
            .grant(Permission.CRUD, UserRole.JUDGE)
            .grant(Permission.R, UserRole.CIVIL_CASE_WORKER)
            .fields()
            .page("make-judicial-decision")
                .pageLabel("Confirm")
                .label("confirmation-info-judicial-decision-made", "You are about to make a judicial decision")
            .done();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
    }


}
