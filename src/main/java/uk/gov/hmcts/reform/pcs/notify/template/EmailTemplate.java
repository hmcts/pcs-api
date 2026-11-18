package uk.gov.hmcts.reform.pcs.notify.template;

import lombok.Getter;

@Getter
public enum EmailTemplate {
    // Respond to Claim Email Templates
    RESPONSE_NO_COUNTERCLAIM("response-no-counterclaim"),
    RESPONSE_NO_COUNTERCLAIM_V2("response-no-counterclaim"),
    RESPONSE_NO_COUNTERCLAIM_LEGAL_REP("response-no-counterclaim-legal-rep"),
    RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED("counterclaim-payment-required"),
    RESPONSE_WITH_COUNTERCLAIM_PAYMENT_REQUIRED_LEGAL_REP("counterclaim-payment-required-legal-rep"),
    COUNTERCLAIM_PAYMENT_SUCCESS("counterclaim-payment-success"),
    COUNTERCLAIM_PAYMENT_SUCCESS_LEGAL_REP("counterclaim-payment-success-legal-rep"),
    RESPONSE_WITH_COUNTERCLAIM_NO_PAYMENT_REQUIRED("counterclaim-no-payment-required"),
    RESPONSE_SUBMITTED_COUNTERCLAIM_NOT_SUBMITTED("counterclaim-not-submitted-yet"),

    // Make a Claim Email Templates
    MAKE_A_CLAIM_CLAIM_SAVED_FOR_LATER("make-a-claim-claim-saved-for-later"),
    MAKE_A_CLAIM_DEFENDANT_MADE_COUNTERCLAIM("make-a-claim-defendant-made-counterclaim"),
    MAKE_A_CLAIM_DEFENDANT_RESPONSE_RECEIVED("make-a-claim-defendant-response-received"),
    MAKE_A_CLAIM_CLAIM_ISSUED("make-a-claim-claim-issued"),

    // Gen App Email Templates
    GENERAL_APPLICATION_RECEIVED("general-application-received"),

    // Hearing Notice Email Template
    NOTICE_OF_HEARING("notice-of-hearing"),

    // Notice of Change Email Templates
    NOTICE_OF_CHANGE_COMPLETED("notice-of-change-completed"),
    NOTICE_OF_CHANGE_COMPLETE_LEGAL_REP("notice-of-change-complete-legal-rep"),
    NOTICE_OF_CHANGE_NO_LONGER_REPRESENTING("notice-of-change-no-longer-representing"),
    NOTICE_OF_CHANGE_OTHER_PARTY_REPRESENTED("notice-of-change-other-party-represented"),

    // Warrant Application Decision Templates
    WARRANT_APP_APPROVED("warrant-appl-approved"),
    WARRANT_APP_REFUSED_PARTY_CIRCUMSTANCES("warrant-appl-refused-party-circumstances"),
    WARRANT_APP_REFUSED_ENFORCE_OUT_OF_TIME("warrant-appl-refused-enforce-out-of-time"),
    WARRANT_APP_REFUSED_NOT_ENFORCEABLE("warrant-appl-refused-not-enforceable"),
    WARRANT_APP_REFUSED_SET_ASIDE_OR_SUSPENDED("warrant-appl-refused-set-aside-or-suspended"),
    WARRANT_APP_REFUSED_BEFORE_POSSESSION_DATE("warrant-appl-refused-before-possession-date"),
    WARRANT_APP_REFUSED_ANOTHER_ENFORCEMENT_ONGOING("warrant-appl-refused-another-enforcment-ongoing"),
    WARRANT_APP_REFUSED_BREATHING_SPACE("warrant-appl-refused-breathing-space"),
    WARRANT_APP_REFUSED_OTHER_REASON("warrant-appl-refused-other-reason"),

    // Writ Application Decision Templates
    WRIT_APP_APPROVED("writ-appl-approved"),
    WRIT_APP_REFUSED_PARTY_CIRCUMSTANCES("writ-appl-refused-party-circumstances"),
    WRIT_APP_REFUSED_ENFORCE_OUT_OF_TIME("writ-appl-refused-enforce-out-of-time"),
    WRIT_APP_REFUSED_NOT_ENFORCEABLE("writ-appl-refused-not-enforceable"),
    WRIT_APP_REFUSED_SET_ASIDE_OR_SUSPENDED("writ-appl-refused-set-aside-or-suspended"),
    WRIT_APP_REFUSED_BEFORE_POSSESSION_DATE("writ-appl-refused-before-possession-date"),
    WRIT_APP_REFUSED_NOT_TFR_TO_HIGH_COURT("writ-appl-refused-not-trf-to-high-court"),
    WRIT_APP_REFUSED_ANOTHER_ENFORCEMENT_ONGOING("writ-appl-refused-another-enforcment-ongoing"),
    WRIT_APP_REFUSED_BREATHING_SPACE("writ-appl-refused-breathing-space"),
    WRIT_APP_REFUSED_OTHER_REASON("writ-appl-refused-other-reason");

    private final String templateKey;

    EmailTemplate(String templateKey) {
        this.templateKey = templateKey;
    }
}
