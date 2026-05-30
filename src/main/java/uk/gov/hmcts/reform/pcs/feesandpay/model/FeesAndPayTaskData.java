package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class FeesAndPayTaskData {

    private final String feeType;

    private final FeeDetails feeDetails;

    private final long caseReference;

    private final String ccdCaseNumber;

    @Builder.Default
    private final Integer volume = 1;

    private final UUID responsiblePartyId;

    private final PaymentCallbackHandlerType paymentCallbackHandlerType;

}
