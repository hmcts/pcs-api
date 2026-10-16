package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimIssuePayment;

@Component
public class MakeAClaimPaymentCallbackHandler extends AbstractCcdPaymentCallbackHandler {

    public MakeAClaimPaymentCallbackHandler(CcdPaymentStateUpdateService ccdPaymentStateUpdateService,
                                            PartyService partyService, ObjectMapper objectMapper) {
        super(ccdPaymentStateUpdateService, partyService, objectMapper);
    }

    @Override
    protected EventId successEventId() {
        return claimIssuePayment;
    }

}
