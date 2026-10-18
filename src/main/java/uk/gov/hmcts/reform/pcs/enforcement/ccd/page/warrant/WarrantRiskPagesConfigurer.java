package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

/**
 * The warrant's risk assessment pages: the risks posed to the bailiff, vulnerable people, access to the property
 * and anything else that could help with the eviction. The warrant application asks them, and "Confirm the
 * eviction details" asks them again so the claimant can change their answers (HDPI-7552). They only show in the
 * warrant flow.
 */
@Component
@AllArgsConstructor
public class WarrantRiskPagesConfigurer implements PageConfigurer {

    private final ViolentAggressiveRiskPage violentAggressiveRiskPage;
    private final VerbalOrWrittenThreatsRiskPage verbalOrWrittenThreatsRiskPage;
    private final ProtestorGroupRiskPage protestorGroupRiskPage;
    private final PoliceOrSocialServicesRiskPage policeOrSocialServicesRiskPage;
    private final FirearmsPossessionRiskPage firearmsPossessionRiskPage;
    private final CriminalAntisocialRiskPage criminalAntisocialRiskPage;
    private final AggressiveAnimalsRiskPage aggressiveAnimalsRiskPage;
    private final PropertyAccessDetailsPage propertyAccessDetailsPage;
    private final VulnerableAdultsChildrenPage vulnerableAdultsChildrenPage;
    private final AdditionalInformationPage additionalInformationPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder
            .add(new LivingInThePropertyPage())
            .add(new EvictionDelayWarningPage())
            .add(new EvictionRisksPosedPage())
            .add(violentAggressiveRiskPage)
            .add(firearmsPossessionRiskPage)
            .add(criminalAntisocialRiskPage)
            .add(verbalOrWrittenThreatsRiskPage)
            .add(protestorGroupRiskPage)
            .add(policeOrSocialServicesRiskPage)
            .add(aggressiveAnimalsRiskPage)
            .add(vulnerableAdultsChildrenPage)
            .add(propertyAccessDetailsPage)
            .add(additionalInformationPage);
    }
}
