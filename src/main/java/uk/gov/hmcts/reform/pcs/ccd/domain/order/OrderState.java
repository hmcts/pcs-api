package uk.gov.hmcts.reform.pcs.ccd.domain.order;

/**
 * Where an order is. An issued order is final: no event moves it on. Nor does any move a historically discarded draft,
 * which is kept as last saved.
 */
public enum OrderState {
    DRAFT,
    SUBMITTED_FOR_REVIEW,
    RETURNED_TO_JUDGE,
    ISSUED,
    /** Retained to read historical rows; no action creates this state. */
    DISCARDED
}
