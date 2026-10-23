package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.bundle;

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
    private Document bundle;

    @CCD(label = "Last updated")
    private String lastUpdated;

    @CCD(label = "Number of pages")
    private Integer pages;

    @CCD(label = "Documents missing from the bundle")
    private List<ListValue<MissingBundleDocument>> missingDocuments;
}
