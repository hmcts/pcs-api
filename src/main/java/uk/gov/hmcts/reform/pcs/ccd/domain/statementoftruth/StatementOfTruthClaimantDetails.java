package uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;

@Data
@Builder
@ComplexType(generate = true)
public class StatementOfTruthClaimantDetails {

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "StatementOfTruthAgreementClaimant"
    )
    private List<StatementOfTruthAgreementClaimant> agreementClaimant;

    @CCD(label = "Full name", max = 60)
    private String fullNameClaimant;

    @CCD(label = "Position or office held", max = 60)
    private String positionClaimant;
}
