package uk.gov.hmcts.reform.pcs.ccd.event.action;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;

@Component
@Slf4j
public class EnforcementPaymentAction implements PaymentSuccessAction {

    @Override
    public EventId eventId() {
        return EventId.enforcementPayment;
    }

    @Override
    public SystemEventResult apply(PaymentStateTaskData taskData) {
        log.info("Enforcement payment confirmed for case {}", taskData.getCaseReference());
        return stateTransition(taskData, State.CASE_PROGRESSION);
    }

}
