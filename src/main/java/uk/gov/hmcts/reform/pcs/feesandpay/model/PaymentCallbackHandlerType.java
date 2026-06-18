package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Getter;

/**
 * Independent of the EventId as there could be distinctions between them.  Example - createPossessionClaim
 * does not have anything to pay at this time so we do not want that as an option for payment.
 * Note also that this is utilised on the callback from the payment to separate out the payment confirmation processing.
 */
@Getter
public enum PaymentCallbackHandlerType {

    CLAIM,
    GEN_APP_ISSUE

}
