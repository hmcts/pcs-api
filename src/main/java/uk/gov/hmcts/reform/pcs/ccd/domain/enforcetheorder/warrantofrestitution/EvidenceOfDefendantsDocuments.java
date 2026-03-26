package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceOfDefendantsDocuments {

    @CCD(
        label = "Type of document",
        typeOverride = FieldType.FixedList,
        typeParameterOverride = "EvidenceDocumentType"
    )
    private EvidenceDocumentType documentType;

    @CCD(label = "Document")
    private Document document;

    @CCD(label = "Short description",
        max = 62
    )
    private String description;
}
