package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenancyLicenceTabDetails {

    @CCD(
        label = "Tenancy, occupation contract or licence agreement type"
    )
    private String typeOfTenancyLicence;

    @CCD(
        label = "Tenancy, occupation contract or licence start date"
    )
    private String tenancyLicenceDate;

    @CCD(
        label = "Do you have a copy of the tenancy or licence agreement?"
    )
    private String hasCopyOfTenancyLicence;

    @CCD(
        label = "Details of why you do not have a copy"
    )
    private String reasonsForNoTenancyLicenceDocuments;

    @CCD(
        label = "Tenancy, occupation contract or licence agreement"
    )
    private List<ListValue<Document>> tenancyLicenceDocuments;
}
