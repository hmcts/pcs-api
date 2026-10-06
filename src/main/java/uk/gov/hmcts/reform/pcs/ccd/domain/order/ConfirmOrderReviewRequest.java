package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the frontend sends when a caseworker finishes reviewing an order, naming the order and the
 * version they reviewed: either a query returning it to the judge, or the order as they issue it and
 * how it is to be issued.
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
     * The caseworker's answers for issuing the order: the order as they issue it, any review dates
     * to add to the case, and how the order is served and sealed. Parties are named by the ids the
     * case context gave them.
     */
    public record Issue(IssuedOrder order,
                        List<ReviewDateEntry> reviewDates,
                        boolean nextStepsComplete,
                        boolean finalOrder,
                        boolean serveAllParties,
                        List<String> partiesToServe,
                        OrderSeal seal) {

        public Issue {
            reviewDates = reviewDates == null ? List.of() : reviewDates;
            partiesToServe = partiesToServe == null ? List.of() : partiesToServe;
        }
    }

    /**
     * The order as the caseworker issues it: the judge's make order form and order document, with
     * any changes the caseworker made to either, and the document's wording as Docweave exports it
     * to HTML, which becomes the issued order.
     */
    public record IssuedOrder(String orderType, Map<String, Object> formData, JsonNode docweaveSnapshot,
                              String html) {
    }

    public record ReviewDateEntry(LocalDate date, ReviewReason reason, String description) {
    }
}
