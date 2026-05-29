package uk.gov.hmcts.reform.pcs.feesandpay.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateServiceRequestPayload {

    @NotNull
    private Long caseReference;
    @NotNull
    private String feeType;

}
