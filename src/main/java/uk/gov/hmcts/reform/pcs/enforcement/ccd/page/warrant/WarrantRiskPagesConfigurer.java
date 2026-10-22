package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;

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
