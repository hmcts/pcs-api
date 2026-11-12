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
     * How the order is issued: served on all parties, or without notice on the parties to serve, each one of the
     * case's parties as the start sent them; whether court staff have completed the next steps, without which it is
     * not issued; what it decides about the claim; what it says about costs; and the sums it says one side pays.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Issue(IssuedOrder order, List<ReviewDateEntry> reviewDates, OrderSeal seal,
                        boolean serveAllParties, List<UUID> partiesToServe, boolean nextStepsComplete,
                        ClaimOutcome claimOutcome, OrderCosts costs, List<MoneyJudgment> moneyJudgments) {

        public Issue {
            reviewDates = reviewDates == null ? List.of() : reviewDates;
            partiesToServe = partiesToServe == null ? List.of() : partiesToServe;
            moneyJudgments = moneyJudgments == null ? List.of() : moneyJudgments;
        }

        public Issue(IssuedOrder order, List<ReviewDateEntry> reviewDates, OrderSeal seal, boolean serveAllParties,
                     List<UUID> partiesToServe, boolean nextStepsComplete, ClaimOutcome claimOutcome) {
            this(order, reviewDates, seal, serveAllParties, partiesToServe, nextStepsComplete, claimOutcome, null,
                null);
        }
    }

    /**
     * The order as issued, with any changes the caseworker made: what it decides about each application, the
     * frontend's form state (stored, never read here), its Docweave document, and {@code html}, Docweave's export,
     * issued as is.
     */
    public record IssuedOrder(OrderType orderType, Map<String, Object> formData, JsonNode docweaveSnapshot,
                              String html, List<ApplicationDecision> applicationDecisions) {

        public IssuedOrder {
            applicationDecisions = applicationDecisions == null ? List.of() : applicationDecisions;
        }

        public IssuedOrder(OrderType orderType, Map<String, Object> formData, JsonNode docweaveSnapshot,
                           String html) {
            this(orderType, formData, docweaveSnapshot, html, List.of());
        }
    }

    public record ReviewDateEntry(LocalDate date, ReviewReason reason, String description) {
    }
}
