package uk.gov.hmcts.reform.pcs.ccd.domain;

import java.util.List;
import java.util.stream.Stream;

/** Display model for the bulk claim details tab; every value is already formatted for display. */
public record BulkClaimDetails(
    List<Row> submission,
    List<Section> parties,
    List<PaymentLine> paymentHistory,
    List<Row> mortgage,
    List<AdditionalPayment> additionalPayments,
    List<Row> originalProvider,
    List<Occupant> occupants
) {

    public boolean isEmpty() {
        return Stream.of(submission, parties, paymentHistory, mortgage, additionalPayments, originalProvider, occupants)
            .allMatch(List::isEmpty);
    }

    public record Row(String label, String value) {
    }

    public record Section(String heading, List<Row> rows) {
    }

    public record PaymentLine(String dueDate, String amountDue, String amountPaid, String cumulativeArrears) {
    }

    public record AdditionalPayment(String amount, String reason, String arrears) {
    }

    public record Occupant(String name, String otherNames, String initials) {
    }
}
