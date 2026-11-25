package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;

/** An earlier version of the case bundle. */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BundleVersion {

    @CCD(label = "Bundle")
    private Document bundle;

    @CCD(label = "Made")
    private String made;

    @CCD(label = "Number of pages")
    private Integer pages;
}
