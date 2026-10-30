package uk.gov.hmcts.reform.pcs.ccd.event.action;

import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;

public interface PaymentSuccessAction {

    EventId eventId();

    SystemEventResult apply(PaymentStateTaskData taskData);

    default SystemEventResult stateTransition(PaymentStateTaskData taskData, State state) {
        return SystemEventResult.withStateTransition(
            taskData.getEventId().name(),
            taskData.getEventId().name(),
            "Payment " + taskData.getPaymentReference() + " updated for " + taskData.getCaseReference(),
            state
        );
    }

}
