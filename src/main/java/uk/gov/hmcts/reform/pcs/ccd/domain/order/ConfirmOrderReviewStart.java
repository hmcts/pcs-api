package uk.gov.hmcts.reform.pcs.ccd.domain.order;

/** What the frontend shows a caseworker when they open the confirm order review event: the order and the case. */
public record ConfirmOrderReviewStart(MakeOrderStart.Order order, MakeOrderStart.CaseContext caseContext) {
}
