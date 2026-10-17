package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

/**
 * The fields of the "Allocate case to bailiff" event (HDPI-7356). They are only set during the event: the
 * allocation itself is held on the enforcement record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BailiffAllocationDetails {

    /**
     * "Claimant name vs defendant name" for the event header. The case title above every page already
     * shows the case number and the property address.
     */
    @CCD(label = "Parties", searchable = false)
    private String bailiffAllocationCaseName;

    @CCD(
        label = "Find the bailiff's name",
        hint = "Enter the first 3 characters of the bailiff's name",
        searchable = false
    )
    private StaffUser allocatedBailiff;

    /**
     * The chosen bailiff's email address, looked up once they are chosen so it can be checked before submit.
     */
    @CCD(label = "Email address", searchable = false)
    private String allocatedBailiffEmail;
}
