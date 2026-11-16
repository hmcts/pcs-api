package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/** A document that could not be included in the case bundle, and why. */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissingBundleDocument {

    @CCD(label = "Document")
    private String documentName;

    @CCD(label = "Folder")
    private String folder;

    @CCD(label = "Page in the bundle")
    private Integer page;

    @CCD(label = "Why it is missing")
    private String reason;
}
