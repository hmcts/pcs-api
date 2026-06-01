package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeesAndPayTaskData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private FeeDetails feeDetails;

    private long caseReference;

    private String ccdCaseNumber;

    @Builder.Default
    private Integer volume = 1;

    private UUID responsiblePartyId;

    private PaymentCallbackHandlerType paymentCallbackHandlerType;
}
