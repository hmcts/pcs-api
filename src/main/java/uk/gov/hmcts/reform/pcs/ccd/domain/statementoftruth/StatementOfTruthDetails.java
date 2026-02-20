package uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;

@Data
@Builder
@ComplexType(generate = true)
@NoArgsConstructor
@AllArgsConstructor
public class StatementOfTruthDetails {

    @CCD(label = "Completed by")
    protected StatementOfTruthCompletedBy completedBy;

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "StatementOfTruthAgreementClaimant"
    )
    protected List<StatementOfTruthAgreementClaimant> agreementClaimant;

    @CCD(
        label = "Full name",
        max = 60)
    protected String fullNameClaimant;

    @CCD(
        label = "Position or office held",
        max = 60
    )
    protected String positionClaimant;

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "StatementOfTruthAgreementLegalRep"
    )
    protected List<StatementOfTruthAgreementLegalRep> agreementLegalRep;

    @CCD(
        label = "Full name",
        max = 60
    )
    protected String fullNameLegalRep;

    @CCD(
        label = "Name of firm",
        max = 60
    )
    protected String firmNameLegalRep;

    @CCD(
        label = "Position or office held",
        max = 60
    )
    protected String positionLegalRep;

}

