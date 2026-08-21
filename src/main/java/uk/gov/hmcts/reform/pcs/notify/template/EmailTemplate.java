package uk.gov.hmcts.reform.pcs.notify.template;

import lombok.Getter;

@Getter
public enum EmailTemplate {
    // Respond to Claim Email Templates
    RESPONSE_NO_COUNTERCLAIM("response-no-counterclaim"),
    RESPONSE_NO_COUNTERCLAIM_LEGAL_REP("response-no-counterclaim-legal-rep"),
    RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED("counterclaim-payment-required"),
    RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED_LEGAL_REP("counterclaim-payment-required-legal-rep"),
    COUNTERCLAIM_PAYMENT_SUCCESS("counterclaim-payment-success"),
    COUNTERCLAIM_PAYMENT_SUCCESS_LEGAL_REP("counterclaim-payment-success-legal-rep"),
    RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED("counterclaim-no-payment-required"),
    RESPONSE_SUBMITTED_COUNTERCLAIM_NOT_SUBMITTED_LEGAL_REP("Response submitted - counterclaim not submitted yet"),

    // Make a Claim Email Templates
    MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER("make-a-claim-claim-saved-for-later"),
    MAKE_A_CLAIM_DEFENDANT_MADE_COUNTERCLAIM("make-a-claim-defendant-made-counterclaim"),
    MAKE_A_CLAIM_DEFENDANT_RESPONSE_RECEIVED("make-a-claim-defendant-response-received"),
    MAKE_A_CLAIM_CLAIM_ISSUED("make-a-claim-claim-issued"),

    // Gen App Email Templates
    GENERAL_APPLICATION_RECEIVED("general-application-received");

    private final String templateKey;

    EmailTemplate(String templateKey) {
        this.templateKey = templateKey;
    }
}
