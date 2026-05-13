package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

/**
 * Independent of the EventId as there could be distinctions between them.  Example - createPossessionClaim
 * does not have anything to pay at this time so we do not want that as an option for payment.
 * Note also that this is utilised on the callback from the payment to separate out the payment confirmation processing.
 */
@Getter
public enum JourneyId {

    RESUME_POSSESSION_CLAIM(EventId.resumePossessionClaim.name());

    private final String name;

    JourneyId(String name) {
        this.name = name;
    }
}
