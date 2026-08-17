package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.LegalRepDocumentTypeWales;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalRepDocument {

    @CCD(
        label = "Type of document",
        typeOverride = FieldType.FixedList,
        typeParameterOverride = "LegalRepDocumentType"
    )
    private LegalRepDocumentType legalRepDocumentType;

    @CCD(
        label = "Type of document",
        typeOverride = FieldType.FixedList,
        typeParameterOverride = "LegalRepDocumentTypeWales"
    )
    private LegalRepDocumentTypeWales legalRepDocumentTypeWales;

    @CCD(label = "Document")
    private Document document;

    @CCD(label = "Short description",
        max = 60
    )
    private String description;
}
