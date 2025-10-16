package uk.gov.hmcts.reform.pcs.feesandpay.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeesAndPayTaskData implements Serializable {

    @NonNull @NotNull
    private String feeType;

    @NonNull @NotNull
    private String caseReference;

    @NonNull @NotNull
    private String ccdCaseNumber;

    @NonNull @NotNull @Builder.Default
    private Integer volume = 1;

    @NonNull @NotNull
    private String responsibleParty;
}
