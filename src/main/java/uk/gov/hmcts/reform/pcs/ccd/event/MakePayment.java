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

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.makePayment;

@Component
@AllArgsConstructor
public class MakePayment implements CCDConfig<PcsCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(makePayment.name(), this::submit)
            .forStateTransition(State.PendingCaseIssued, State.CaseIssued)
            .name("Make payment")
            .grant(Permission.CRUD, UserRole.APPLICANT_SOLICITOR)
            .grant(Permission.R, UserRole.CIVIL_CASE_WORKER)
            .endButtonLabel("Pay Now")
            .fields()
            .page("submit-claim")
                .pageLabel("Complete payment")
                .label("payment-info", "You are about to pay the claim fee")
            .done();
    }

    private void submit(EventPayload<PcsCase, State> eventPayload) {
    }


}
