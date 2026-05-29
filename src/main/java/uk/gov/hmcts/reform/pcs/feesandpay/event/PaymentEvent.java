package uk.gov.hmcts.reform.pcs.feesandpay.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.payment;

@Component
@AllArgsConstructor
@Slf4j
public class PaymentEvent implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(payment.name(), this::submit)
            .forStates(State.PENDING_CASE_ISSUED, State.CASE_ISSUED)
            .name("Payment Confirmation")
            .showCondition(ShowConditions.NEVER_SHOW)
            .grant(Permission.CRU, UserRole.SYSTEM_USER);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Received: {}", eventPayload);
        return SubmitResponse.<State>builder().state(State.CASE_ISSUED).build();
    }

}
