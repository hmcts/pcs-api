package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

/** The Bundle tab: the case bundle, whether it is up to date, and what is missing from it. */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class BundleTab {

    @CCD(label = "Status")
    private String status;

    @CCD(label = "Bundle")
    // Never read back: under the bundleTab_ prefix Jackson would bind none of the Document's fields.
    // The tab is rebuilt on every case view.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Document bundle;

    @CCD(label = "Last updated")
    private String lastUpdated;

    @CCD(label = "Number of pages")
    private Integer pages;

    @CCD(label = "Documents missing from the bundle")
    private List<ListValue<MissingBundleDocument>> missingDocuments;
}
