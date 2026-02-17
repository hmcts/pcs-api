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
public class StatementOfTruthLegalRepDetails {

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "StatementOfTruthAgreementLegalRep"
    )
    private List<StatementOfTruthAgreementLegalRep> agreementLegalRep;

    @CCD(label = "Full name", max = 60)
    private String fullNameLegalRep;

    @CCD(label = "Name of firm", max = 60)
    private String firmNameLegalRep;

    @CCD(label = "Position or office held", max = 60)
    private String positionLegalRep;
}
