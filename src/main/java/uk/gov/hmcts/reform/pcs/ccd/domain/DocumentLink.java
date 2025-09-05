package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

@Data
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentLink {

    @CCD(
        label = "File",
        hint = "Only PDF, DOC, and JPEG files are allowed",
        regex = ".pdf,.doc,.jpg,.jpeg",
        typeOverride = FieldType.Document,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private Document documentLink;

    @JsonCreator
    public DocumentLink(@JsonProperty("documentLink") Document documentLink) {
        this.documentLink = documentLink;
    }
}
