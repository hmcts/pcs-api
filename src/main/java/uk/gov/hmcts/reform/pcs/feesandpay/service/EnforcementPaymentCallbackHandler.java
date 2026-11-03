package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.system.SystemAction;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforcementPayment;
import static uk.gov.hmcts.reform.pcs.system.SystemAction.ENFORCEMENT_PAYMENT;

@Component
public class EnforcementPaymentCallbackHandler extends AbstractCcdPaymentCallbackHandler {

    public EnforcementPaymentCallbackHandler(CcdPaymentStateUpdateService ccdPaymentStateUpdateService,
                                             PartyService partyService, ObjectMapper objectMapper) {
        super(ccdPaymentStateUpdateService, partyService, objectMapper);
    }

    @Override
    protected EventId successEventId() {
        return enforcementPayment;
    }

    @Override
    protected SystemAction actionId() {
        return ENFORCEMENT_PAYMENT;
    }

}
