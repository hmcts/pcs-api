package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdCircumstances {

    @CCD
    private YesOrNo dependantChildren;

    @CCD
    private YesOrNo shareAdditionalCircumstances;

    @CCD
    private String additionalCircumstancesDetails;

    @CCD
    private YesOrNo exceptionalHardship;

    @CCD
    private String exceptionalHardshipDetails;

}
