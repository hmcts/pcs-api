package uk.gov.hmcts.reform.pcs.feesandpay.model;

import java.util.UUID;

public record BulkPbaDetails(
    String pbaAccountNumber,
    String pbaCustomerReference,
    String organisationName,
    UUID bulkClaimSubmissionId
) {
}
