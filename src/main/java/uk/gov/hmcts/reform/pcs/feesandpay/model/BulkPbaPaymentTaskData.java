package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class BulkPbaPaymentTaskData {

    private final String serviceRequestReference;

    private final long caseReference;

    private final String pbaAccountNumber;

    private final String pbaCustomerReference;

    private final String organisationName;

    private final UUID bulkClaimSubmissionId;

    private final BigDecimal amount;

}
