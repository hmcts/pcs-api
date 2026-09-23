package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.ClaimantDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.DefendantDocumentTypeWales;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalRepDocument {

    @CCD(
        label = "Type of document",
        typeOverride = FieldType.FixedList,
        typeParameterOverride = "ClaimantDocumentType"
    )
    private ClaimantDocumentType claimantDocumentType;

    @CCD(
        label = "Type of document",
        typeOverride = FieldType.FixedList,
        typeParameterOverride = "ClaimantDocumentTypeWales"
    )
    private ClaimantDocumentTypeWales claimantDocumentTypeWales;

    @CCD(
        label = "Type of document",
        typeOverride = FieldType.FixedList,
        typeParameterOverride = "DefendantDocumentType"
    )
    private DefendantDocumentType defendantDocumentType;

    @CCD(
        label = "Type of document",
        typeOverride = FieldType.FixedList,
        typeParameterOverride = "DefendantDocumentTypeWales"
    )
    private DefendantDocumentTypeWales defendantDocumentTypeWales;

    @CCD(label = "Document")
    private Document document;

    @CCD(label = "Short description",
        max = 60
    )
    private String description;

    @CCD(showCondition = ShowConditions.NEVER_SHOW)
    private String contentType;

    @CCD(showCondition = ShowConditions.NEVER_SHOW)
    private Long sizeInBytes;
}
