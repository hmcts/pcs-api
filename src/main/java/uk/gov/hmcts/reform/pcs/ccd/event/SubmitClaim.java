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

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.submitClaim;

@Component
@AllArgsConstructor
public class SubmitClaim implements CCDConfig<PcsCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(submitClaim.name(), this::submit)
            .forStateTransition(State.Draft, State.PendingCaseIssued)
            .name("Submit claim")
            .grant(Permission.CRUD, UserRole.APPLICANT_SOLICITOR)
            .grant(Permission.R, UserRole.CIVIL_CASE_WORKER)
            .endButtonLabel("Submit")
            .fields()
            .page("submit-claim")
                .pageLabel("Confirm submission")
                .label("submission-info", "You are about to submit the claim")
            .done();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
    }


}
