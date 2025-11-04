package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableAdultsChildren.VULNERABLE_PEOPLE_YES_NO_LABEL;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

/**
 * The main domain model representing an enforcement order.
 */
@Builder
@Data
public class EnforcementOrder {

    @CCD(
        label = "What do you want to apply for?"
    )
    private SelectEnforcementType selectEnforcementType;

    @CCD
    private NameAndAddressForEviction nameAndAddressForEviction;

    @CCD(
        label = "Does anyone living at the property pose a risk to the bailiff?"
    )
    private YesNoNotSure anyRiskToBailiff;

    @CCD(
            label = VULNERABLE_PEOPLE_YES_NO_LABEL
    )
    private YesNoNotSure vulnerablePeopleYesNo;
    
    @CCD
    private VulnerableAdultsChildren vulnerableAdultsChildren;

    @CCD(
        label = "Test: Are you also making a claim? (Branch 2506 pattern test)"
    )
    private YesNoNotSure testProhibitedConductClaim;

    @CCD
    private PeriodicContractTermsWalesTest periodicContractTermsWalesTest;

}
