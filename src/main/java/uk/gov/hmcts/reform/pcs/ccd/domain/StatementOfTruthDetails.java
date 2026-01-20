package uk.gov.hmcts.reform.pcs.ccd.domain;

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
    private StatementOfTruthCompletedBy completedBy;

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "StatementOfTruthAgreementClaimant"
    )
    private List<StatementOfTruthAgreementClaimant> agreementClaimant;

    @CCD(label = "Full name")
    private String fullNameClaimant;

    @CCD(label = "Position or office held")
    private String positionClaimant;

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "StatementOfTruthAgreementLegalRep"
    )
    private List<StatementOfTruthAgreementLegalRep> agreementLegalRep;

    @CCD(label = "Full name")
    private String fullNameLegalRep;

    @CCD(label = "Name of firm")
    private String firmNameLegalRep;

    @CCD(label = "Position or office held")
    private String positionLegalRep;

}

