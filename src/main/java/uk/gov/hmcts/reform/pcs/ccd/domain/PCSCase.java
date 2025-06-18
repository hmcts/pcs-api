package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {

    @CCD(
        label = "Applicant's forname",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String applicantForename;

    @CCD(
        label = "Applicant's surname",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String applicantSurname;

    @CCD(
        label = "Property address",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private AddressUK propertyAddress;

}
