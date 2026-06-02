package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DefendantAccess;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalDocument {

    @CCD(
        label = "Type of document",
        typeOverride = FieldType.FixedList,
        typeParameterOverride = "AdditionalDocumentType",
        access = {DefendantAccess.class}
    )
    private AdditionalDocumentType documentType;

    @CCD(label = "Document", access = {DefendantAccess.class})
    private Document document;

    @CCD(label = "Short description",
        typeOverride = FieldType.TextArea,
        access = {DefendantAccess.class}
    )
    private String description;
}
