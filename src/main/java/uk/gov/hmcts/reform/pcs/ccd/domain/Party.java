package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    @CCD(access = {CitizenAccess.class})
    private String firstName;

    @CCD(access = {CitizenAccess.class})
    private String lastName;

    @CCD(access = {CitizenAccess.class})
    private String orgName;

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo nameKnown;

    // emailAddress: NO CitizenAccess annotation - stored in DB but NOT exposed to citizens
    private String emailAddress;

    @CCD(access = {CitizenAccess.class})
    private AddressUK address;

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo addressKnown;

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo addressSameAsProperty;

    @CCD(access = {CitizenAccess.class})
    private String phoneNumber;

    @CCD(access = {CitizenAccess.class})
    private VerticalYesNo phoneNumberProvided;

}
