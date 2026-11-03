package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.system.SystemAction;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.system.SystemAction.CLAIM_ISSUED_PAYMENT;

@Component
public class MakeAClaimPaymentCallbackHandler extends AbstractCcdPaymentCallbackHandler {

    public MakeAClaimPaymentCallbackHandler(CcdPaymentStateUpdateService ccdPaymentStateUpdateService,
                                            PartyService partyService, ObjectMapper objectMapper) {
        super(ccdPaymentStateUpdateService, partyService, objectMapper);
    }

    @Override
    protected EventId successEventId() {
        return resumePossessionClaim;
    }

    @Override
    protected SystemAction actionId() {
        return CLAIM_ISSUED_PAYMENT;
    }

}
