package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstances {

    @CCD(access = {CitizenAccess.class})
    private YesOrNo dependantChildren;

    @CCD(access = {CitizenAccess.class})
    private YesOrNo shareAdditionalCircumstances;

    @CCD(max = 500,access = {CitizenAccess.class})
    private String additionalCircumstancesDetails;

    @CCD(access = {CitizenAccess.class})
    private YesOrNo exceptionalHardship;

    @CCD(max = 500,access = {CitizenAccess.class})
    private String exceptionalHardshipDetails;

    @CCD(max = 500,access = {CitizenAccess.class})
    private String dependantChildrenDetails;

    @CCD(access = {CitizenAccess.class})
    private YesOrNo otherDependants;

    @CCD(max = 500,access = {CitizenAccess.class})
    private String otherDependantDetails;

    @CCD(access = {CitizenAccess.class})
    private YesOrNo otherTenants;

    @CCD(max = 500,access = {CitizenAccess.class})
    private String otherTenantsDetails;

    @CCD(access = {CitizenAccess.class})
    private YesNoNotSure alternativeAccommodation;

    @CCD(access = {CitizenAccess.class})
    private LocalDate alternativeAccommodationTransferDate;

}
