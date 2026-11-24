package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;

/**
 * Derives which alternatives-to-possession detail pages apply for the selected alternatives.
 */
@Component
public class AlternativesRules {

    /**
     * Visibility of the three alternatives detail pages.
     */
    public record AlternativesPageFlags(YesOrNo showSuspensionOfRightToBuyHousingActsPage,
                                        YesOrNo showDemotionOfTenancyHousingActsPage,
                                        YesOrNo showSuspensionToBuyDemotionOfTenancyPages) {
    }

    public AlternativesPageFlags derivePageFlags(Set<AlternativesToPossession> alternatives) {
        boolean suspension = alternatives.contains(SUSPENSION_OF_RIGHT_TO_BUY);
        boolean demotion = alternatives.contains(DEMOTION_OF_TENANCY);

        return new AlternativesPageFlags(
            YesOrNo.from(suspension && !demotion),
            YesOrNo.from(demotion && !suspension),
            YesOrNo.from(suspension && demotion)
        );
    }

    /**
     * Applies the derived flags to whichever alternatives sections are present on the case.
     */
    public void applyPageFlags(PCSCase caseData) {
        AlternativesPageFlags flags = derivePageFlags(caseData.getAlternativesToPossession());

        if (caseData.getSuspensionOfRightToBuy() != null) {
            caseData.getSuspensionOfRightToBuy()
                .setShowHousingActsPage(flags.showSuspensionOfRightToBuyHousingActsPage());
        }
        if (caseData.getDemotionOfTenancy() != null) {
            caseData.getDemotionOfTenancy().setShowHousingActsPage(flags.showDemotionOfTenancyHousingActsPage());
        }
        if (caseData.getSuspensionOfRightToBuyDemotionOfTenancy() != null) {
            caseData.getSuspensionOfRightToBuyDemotionOfTenancy()
                .setSuspensionToBuyDemotionOfTenancyPages(flags.showSuspensionToBuyDemotionOfTenancyPages());
        }
    }
}
