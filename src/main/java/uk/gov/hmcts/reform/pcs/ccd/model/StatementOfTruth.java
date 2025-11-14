package uk.gov.hmcts.reform.pcs.ccd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatementOfTruth {

    private StatementOfTruthCompletedBy completedBy;

    private StatementOfTruthAgreementClaimant agreementClaimant;

    private String fullNameClaimant;

    private String positionClaimant;

    private StatementOfTruthAgreementLegalRep agreementLegalRep;

    private String fullNameLegalRep;

    private String firmNameLegalRep;

    private String positionLegalRep;

}

