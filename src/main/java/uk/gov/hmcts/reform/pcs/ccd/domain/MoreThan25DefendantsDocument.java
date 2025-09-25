package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoreThan25DefendantsDocument {
    @CCD(
        label = "Type of document",
        typeOverride = FixedList,
        typeParameterOverride = "MoreThan25DefendantsDocumentType",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private MoreThan25DefendantsDocumentType documentCategory;

    @CCD(
        label = "Add document",
        hint = "Upload a document to the system",
        typeParameterOverride = "Document",
        regex = ".pdf, .docx",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private Document documentLink;

    @CCD(
        label = "Short description",
        typeOverride = TextArea,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String shortDescription;
}
