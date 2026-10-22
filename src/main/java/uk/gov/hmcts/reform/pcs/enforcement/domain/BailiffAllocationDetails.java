package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BailiffAllocationDetails {

    @CCD(label = "Parties", searchable = false)
    private String bailiffAllocationCaseName;

    @CCD(
        label = "Find the bailiff's name",
        hint = "Enter the first 3 characters of the bailiff's name",
        searchable = false
    )
    private StaffUser allocatedBailiff;

    @CCD(label = "Email address", searchable = false)
    private String allocatedBailiffEmail;
}
