package uk.gov.hmcts.reform.pcs.ccd.domain.order;

/**
 * Where an order is. An issued order is final: no event moves it on. Nor does any move a draft its judge discarded,
 * which is kept as they last saved it.
 */
public enum OrderState {
    DRAFT,
    SUBMITTED_FOR_REVIEW,
    RETURNED_TO_JUDGE,
    ISSUED,
    DISCARDED
}
