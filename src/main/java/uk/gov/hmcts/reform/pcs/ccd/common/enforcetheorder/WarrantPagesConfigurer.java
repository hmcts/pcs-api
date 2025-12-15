package uk.gov.hmcts.reform.pcs.ccd.common.enforcetheorder;

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
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementApplicationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.AdditionalInformationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.AggressiveAnimalsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.ChangeNameAddressPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.CriminalAntisocialRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.EvictionDelayWarningPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.EvictionRisksPosedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.FirearmsPossessionRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LandRegistryFeesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LanguageUsedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LegalCostsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LivingInThePropertyPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.MoneyOwedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.NameAndAddressForEvictionPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.PeopleWhoWillBeEvictedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.PeopleYouWantToEvictPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.PoliceOrSocialServicesRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.PropertyAccessDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.ProtestorGroupRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.RepaymentsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.StatementOfTruthPlaceHolder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.VerbalOrWrittenThreatsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.ViolentAggressiveRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.VulnerableAdultsChildrenPage;

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
            .add(new EnforcementApplicationPage())
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