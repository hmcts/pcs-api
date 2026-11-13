package uk.gov.hmcts.reform.pcs.ccd.domain.order;

/** What an order says about costs. Any sum to be paid is one of its money judgments. */
public enum OrderCosts {
    /** The defendant pays the claimant's costs, without the order saying how they are assessed. */
    DEFENDANT_PAYS,
    DEFENDANT_PAYS_FIXED,
    DEFENDANT_PAYS_SUMMARY,
    CLAIMANT_PAYS_SUMMARY,
    PUBLIC_FUNDING,
    COSTS_IN_CASE,
    RESERVED,
    NO_ORDER,
    OTHER
}
