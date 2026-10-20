package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    /**
     * How the order is issued. The frontend also asks about next steps, whether the order is final and
     * whom to serve it on; nothing acts on those answers yet, so they are not read.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Issue(IssuedOrder order, List<ReviewDateEntry> reviewDates, OrderSeal seal) {

        public Issue {
            reviewDates = reviewDates == null ? List.of() : reviewDates;
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
