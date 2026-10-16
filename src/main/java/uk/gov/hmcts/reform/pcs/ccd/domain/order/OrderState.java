package uk.gov.hmcts.reform.pcs.ccd.domain.order;

/** Where an order is. An issued order is final: no event moves it on. */
public enum OrderState {
    DRAFT,
    SUBMITTED_FOR_REVIEW,
    RETURNED_TO_JUDGE,
    ISSUED
}
