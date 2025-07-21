package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserType;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {

    @CCD(
        label = "Applicant's forename",
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

    @CCD(
        label = "What type of user are you?", 
        typeOverride = FieldType.FixedRadioList, 
        typeParameterOverride = "UserType", 
        access = { CitizenAccess.class, CaseworkerAccess.class })

    private UserType userType;

    // Field to store the rendered userTypeInfo markdown
    private String userTypeInfoMarkdown;

}
