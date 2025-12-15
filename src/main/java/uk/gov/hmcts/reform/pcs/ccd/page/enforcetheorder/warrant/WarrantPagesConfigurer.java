package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.common.PagesConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Slf4j
@Component
@AllArgsConstructor
public class WarrantPagesConfigurer implements PagesConfigurer {

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
    private final SavingPageBuilderFactory savingPageBuilderFactory;

    @Override
    public void configurePages(Event.EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        PageBuilder pageBuilder = savingPageBuilderFactory.create(eventBuilder, enforceTheOrder);
        pageBuilder
            .add(new NameAndAddressForEvictionPage())
            .add(new ChangeNameAddressPage())
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
            .add(new LandRegistryFeesPage())
            .add(new RepaymentsPage())
            .add(new LanguageUsedPage())
            .add(new StatementOfTruthPlaceHolder());
    }
}