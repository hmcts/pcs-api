package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;

@Data
@Builder
@ComplexType(generate = true)
public class StatementOfTruthDetails {

    @CCD(label = "Completed by")
    private StatementOfTruthCompletedBy completedBy;

    @CCD
    private StatementOfTruthClaimantDetails claimantDetails;

    @CCD
    private StatementOfTruthLegalRepDetails legalRepDetails;

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "EnforcementStatementOfTruthAgreement"
    )
    private List<EnforcementStatementOfTruthAgreement> enforcementCertification;

}

