package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

import java.util.List;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {

    @CCD(ignore = true)
    @JsonIgnore
    private Long ccdCaseReference;

    @CCD(label = "Applicant's first name")
    private String applicantForename;

    private AddressUK propertyAddress;

    @CCD(label = "General Applications")
    private List<GeneralApplication> generalApplicationList;
}
