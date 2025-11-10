package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeesAndPayTaskData implements Serializable {

    private String feeType;

    private String caseReference;

    private String ccdCaseNumber;

    @Builder.Default
    private Integer volume = 1;

    private String responsibleParty;
}
