package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegalRepDocumentUpload {

    @CCD(
        label = "Do these documents relate to an existing application?"
    )
    @JsonProperty("DocumentUploadCategories")
    private DocumentUploadCategory uploadCategories;

    @CCD(
        label = "Add document",
        hint = "Upload a document to the system"
    )
    private List<ListValue<LegalRepDocument>> additionalDocuments;
}
