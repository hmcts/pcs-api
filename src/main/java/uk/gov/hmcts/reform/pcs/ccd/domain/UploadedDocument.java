package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedDocument {

    @CCD(ignore = true)
    private static final String ACCEPT_TYPES = ".doc,.docx,.xls,.xlsm,.ppt,.pptx,.pdf,.rtf,.txt,.csv,"
        + ".jpg,.jpeg,.png,.bmp,.tif,.tiff";

    @CCD(
        label = "Type of document",
        typeOverride = FieldType.FixedList,
        typeParameterOverride = "AdditionalDocumentType"
    )
    private AdditionalDocumentType documentType;

    @CCD(
        label = "Document",
        // Note this regex attribute is not actually interpreted as a regex for the Document type
        regex = ACCEPT_TYPES,
        access = {CitizenAccess.class}
    )
    private Document document;

    @CCD(showCondition = ShowConditions.NEVER_SHOW)
    private String contentType;

    @CCD(showCondition = ShowConditions.NEVER_SHOW)
    private Long sizeInBytes;

}
