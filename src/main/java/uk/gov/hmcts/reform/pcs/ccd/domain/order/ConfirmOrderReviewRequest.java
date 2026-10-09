package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewReason;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** What the frontend sends when a caseworker returns an order to its judge with a query, or issues it. */
public record ConfirmOrderReviewRequest(Action action,
                                        UUID orderId,
                                        long version,
                                        String taskId,
                                        String queryToJudge,
                                        Issue issue) {

    public enum Action {
        RETURN_TO_JUDGE,
        ISSUE
    }

    /** How the order is issued. Parties are named by the ids the case context gave them. */
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
     * The order as issued, with any changes the caseworker made; {@code html} is Docweave's export,
     * issued as is.
     */
    public record IssuedOrder(OrderType orderType, Map<String, Object> formData, JsonNode docweaveSnapshot,
                              String html) {
    }

    public record ReviewDateEntry(LocalDate date, ReviewReason reason, String description) {
    }
}
