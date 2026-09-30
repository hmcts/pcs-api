package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What the frontend sends when a caseworker finishes reviewing an order, naming the order and the
 * version they reviewed: either a query returning it to the judge, or how it is to be issued.
 */
public record ConfirmOrderReviewRequest(Action action,
                                        UUID orderId,
                                        long version,
                                        String queryToJudge,
                                        Issue issue) {

    public enum Action {
        RETURN_TO_JUDGE,
        ISSUE
    }

    /**
     * The caseworker's answers for issuing the order: any review dates to add to the case, and how
     * the order is served and sealed. Parties are named by the ids the case context gave them.
     */
    public record Issue(List<ReviewDateEntry> reviewDates,
                        boolean nextStepsComplete,
                        boolean finalOrder,
                        boolean serveAllParties,
                        List<String> partiesToServe,
                        OrderSeal seal) {
    }

    public record ReviewDateEntry(LocalDate date, ReviewReason reason, String description) {
    }
}
