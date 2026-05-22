package uk.gov.hmcts.reform.pcs.notify.template;

import lombok.Getter;

@Getter
public enum EmailTemplate {
    RESPONSE_NO_COUNTERCLAIM("response-no-counterclaim"),
    RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED("counterclaim-payment-required"),
    COUNTERCLAIM_PAYMENT_SUCCESS("counterclaim-payment-success"),
    RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED("counterclaim-no-payment-required");

    private final String templateKey;

    EmailTemplate(String templateKey) {
        this.templateKey = templateKey;
    }
}
