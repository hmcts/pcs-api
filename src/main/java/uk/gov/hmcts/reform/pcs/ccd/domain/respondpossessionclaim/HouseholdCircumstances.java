package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstances {

    @CCD
    private YesOrNo dependantChildren;

    @CCD(max = 500)
    private String dependantChildrenDetails;

    @CCD
    private YesOrNo otherDependants;

    @CCD(max = 500)
    private String otherDependantDetails;

    @CCD
    private YesOrNo otherTenants;

    @CCD(max = 500)
    private String otherTenantsDetails;

    @CCD
    private YesNoNotSure alternativeAccommodation;

    @CCD
    private LocalDate alternativeAccommodationTransferDate;

}
