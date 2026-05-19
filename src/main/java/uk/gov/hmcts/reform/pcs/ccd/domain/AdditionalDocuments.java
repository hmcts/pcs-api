package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalDocuments {

    @CCD(
        label = "Type of document",
        access = {CitizenAccess.class}
    )
    private DynamicList documentTypeList;

    @CCD(label = "Document", access = {CitizenAccess.class})
    private Document document;

    @CCD(label = "Short description",
            typeOverride = FieldType.TextArea,
            access = {CitizenAccess.class}
    )
    private String description;
}
