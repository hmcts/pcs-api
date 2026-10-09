package uk.gov.hmcts.reform.pcs.ccd.domain.order;

/**
 * Where an order is, from a judge's draft to an issued order. A judge's draft is theirs to change
 * until they submit it; court staff then return it to the judge or issue it; a returned order is the
 * judge's to change and resubmit. An issued order is final: no event moves it on.
 */
public enum OrderState {
    DRAFT,
    SUBMITTED_FOR_REVIEW,
    RETURNED_TO_JUDGE,
    ISSUED
}
