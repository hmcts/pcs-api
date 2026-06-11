package uk.gov.hmcts.reform.pcs.notify.template;

import lombok.Getter;

@Getter
public enum EmailTemplate {
    // Respond to Claim Email Templates
    RESPONSE_NO_COUNTERCLAIM("response-no-counterclaim"),
    RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED("counterclaim-payment-required"),
    COUNTERCLAIM_PAYMENT_SUCCESS("counterclaim-payment-success"),
    RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED("counterclaim-no-payment-required"),

    // Make a Claim Email Templates
    MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER("make-a-claim-claim-saved-for-later"),
    MAKE_A_CLAIM_DEFENDANT_MADE_COUNTERCLAIM("make-a-claim-defendant-made-counterclaim"),
    MAKE_A_CLAIM_DEFENDANT_RESPONSE_RECEIVED("make-a-claim-defendant-response-received");

    private final String templateKey;

    EmailTemplate(String templateKey) {
        this.templateKey = templateKey;
    }
}
