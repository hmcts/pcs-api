package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;

import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@SuppressWarnings("squid:S1192")
public class StatementOfTruthDetails {

    @CCD(label = "Completed by")
    private StatementOfTruthCompletedBy completedBy;

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "StatementOfTruthAgreementClaimant"
    )
    private List<StatementOfTruthAgreementClaimant> agreementClaimant;

    @CCD(label = "Full name", max = 60)
    private String fullNameClaimant;

    @CCD(label = "Position or office held", max = 60)
    private String positionClaimant;

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

    @CCD(
        typeOverride = MultiSelectList,
        typeParameterOverride = "StatementOfTruthAgreement"
    )
    private List<StatementOfTruthAgreement> certification;
}

