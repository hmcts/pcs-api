package uk.gov.hmcts.reform.pcs.ccd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatementOfTruth {

    private String completedBy;

    private List<String> agreementClaimant;

    private String fullNameClaimant;

    private String positionClaimant;

    private List<String> agreementLegalRep;

    private String fullNameLegalRep;

    private String firmNameLegalRep;

    private String positionLegalRep;

}

