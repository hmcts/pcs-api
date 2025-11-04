package uk.gov.hmcts.reform.pcs.ccd.service.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.RENT_ARREARS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.DEMOTED_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.INTRODUCTORY_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.OTHER;

/**
 * Routing policy for Introductory, Demoted, and Other Tenancy types.
 * Shows Rent Details page when grounds for possession are selected (YES)
 * AND RENT_ARREARS ground is selected.
 */
@Component
@Slf4j
public class IntroductoryDemotedOtherRoutingPolicy implements RentDetailsRoutingPolicy {

    @Override
    public YesOrNo shouldShowRentDetails(PCSCase caseData) {
        if (caseData.getHasIntroductoryDemotedOtherGroundsForPossession() != VerticalYesNo.YES) {
            return YesOrNo.NO;
        }

        Set<IntroductoryDemotedOrOtherGrounds> grounds = caseData.getIntroductoryDemotedOrOtherGrounds();
        if (grounds == null || grounds.isEmpty()) {
            return YesOrNo.NO;
        }

        boolean hasRentArrears = grounds.contains(RENT_ARREARS);
        return YesOrNo.from(hasRentArrears);
    }

    @Override
    public boolean supports(TenancyLicenceType tenancyType) {
        return INTRODUCTORY_TENANCY == tenancyType
            || DEMOTED_TENANCY == tenancyType
            || OTHER == tenancyType;
    }
}

