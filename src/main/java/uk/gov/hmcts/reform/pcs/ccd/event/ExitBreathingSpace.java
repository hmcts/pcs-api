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

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.exitBreathingSpace;

@Component
@AllArgsConstructor
public class ExitBreathingSpace implements CCDConfig<PcsCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(exitBreathingSpace.name(), this::submit)
            .forStateTransition(State.BreathingSpace, State.CaseIssued)
            .name("Exit breathing space")
            .grant(Permission.CRUD, UserRole.RESPONDENT_SOLICITOR, UserRole.CIVIL_CASE_WORKER)
            .fields()
            .page("exit-breathing-space")
                .pageLabel("Confirm")
                .label("confirmation-info-exit", "You are about to exit breathing space")
            .done();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
        System.out.println("#### Exited breathing space");
    }


}
