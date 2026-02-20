package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;

@Slf4j
@Component
@AllArgsConstructor
public class WarrantPageConfigurer implements EnforcementPageConfigurer {

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
    private final LandRegistryFeesPage landRegistryFeesPage;
    private final DefendantsDOBPage defendantsDOBPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        configureInitialPages(pageBuilder);
        pageBuilder
            .add(new NameAndAddressForEvictionPage())
            .add(new ChangeNameAddressPage())
            .add(new ConfirmIfDOBKnownPage())
            .add(defendantsDOBPage)
            .add(new PeopleWhoWillBeEvictedPage())
            .add(new PeopleYouWantToEvictPage())
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
            .add(additionalInformationPage)
            .add(new MoneyOwedPage())
            .add(new LegalCostsPage())
            .add(landRegistryFeesPage)
            .add(new RepaymentsPage())
            .add(new LanguageUsedPage())
            .add(new SuspendedOrderPage())
            .add(new StatementOfTruthPage());
    }
}
