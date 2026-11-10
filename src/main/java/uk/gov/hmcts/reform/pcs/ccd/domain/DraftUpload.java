package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * A file uploaded into a draft, returned in the case view so CDAM serves it to the draft's owner: the data store
 * looks for the document's {@code document_url} anywhere in the case data the user can read. Its fields are text,
 * not a Document, so case file view does not list it.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftUpload {

    @JsonProperty("document_url")
    @CCD(label = "Document URL")
    private String documentUrl;

    @JsonProperty("document_binary_url")
    @CCD(label = "Document binary URL")
    private String documentBinaryUrl;

    @JsonProperty("document_filename")
    @CCD(label = "File name")
    private String documentFilename;
}
