package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementApplicationPage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantPageConfigurerTest extends BasePageTest {

    @InjectMocks
    private WarrantPageConfigurer warrantPageConfigurer;

    @Mock
    private ViolentAggressiveRiskPage violentAggressiveRiskPage;

    @Mock
    private VerbalOrWrittenThreatsRiskPage verbalOrWrittenThreatsRiskPage;

    @Mock
    private ProtestorGroupRiskPage protestorGroupRiskPage;

    @Mock
    private PoliceOrSocialServicesRiskPage policeOrSocialServicesRiskPage;

    @Mock
    private FirearmsPossessionRiskPage firearmsPossessionRiskPage;

    @Mock
    private CriminalAntisocialRiskPage criminalAntisocialRiskPage;

    @Mock
    private AggressiveAnimalsRiskPage aggressiveAnimalsRiskPage;

    @Mock
    private PropertyAccessDetailsPage propertyAccessDetailsPage;

    @Mock
    private VulnerableAdultsChildrenPage vulnerableAdultsChildrenPage;

    @Mock
    private AdditionalInformationPage additionalInformationPage;

    @Mock
    private LandRegistryFeesPage landRegistryFeesPage;

    @Mock
    private MoneyOwedPage moneyOwedPage;

    @Mock
    private LegalCostsPage legalCostsPage;

    @Mock
    private RepaymentsPage repaymentsPage;

    @Test
    @SuppressWarnings("squid:S5961")
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        SavingPageBuilder pageBuilder = mock(SavingPageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        warrantPageConfigurer.configurePages(pageBuilder);

        // Then
        InOrder inOrder = inOrder(pageBuilder);
        inOrder.verify(pageBuilder).add(isA(EnforcementApplicationPage.class));
        inOrder.verify(pageBuilder).add(isA(NameAndAddressForEvictionPage.class));
        inOrder.verify(pageBuilder).add(isA(ChangeNameAddressPage.class));
        inOrder.verify(pageBuilder).add(isA(PeopleWhoWillBeEvictedPage.class));
        inOrder.verify(pageBuilder).add(isA(PeopleYouWantToEvictPage.class));
        inOrder.verify(pageBuilder).add(isA(LivingInThePropertyPage.class));
        inOrder.verify(pageBuilder).add(isA(EvictionDelayWarningPage.class));
        inOrder.verify(pageBuilder).add(isA(EvictionRisksPosedPage.class));
        inOrder.verify(pageBuilder).add(violentAggressiveRiskPage);
        inOrder.verify(pageBuilder).add(firearmsPossessionRiskPage);
        inOrder.verify(pageBuilder).add(criminalAntisocialRiskPage);
        inOrder.verify(pageBuilder).add(verbalOrWrittenThreatsRiskPage);
        inOrder.verify(pageBuilder).add(protestorGroupRiskPage);
        inOrder.verify(pageBuilder).add(policeOrSocialServicesRiskPage);
        inOrder.verify(pageBuilder).add(aggressiveAnimalsRiskPage);
        inOrder.verify(pageBuilder).add(vulnerableAdultsChildrenPage);
        inOrder.verify(pageBuilder).add(propertyAccessDetailsPage);
        inOrder.verify(pageBuilder).add(additionalInformationPage);

        inOrder.verify(pageBuilder).add(moneyOwedPage);
        inOrder.verify(pageBuilder).add(legalCostsPage);

        inOrder.verify(pageBuilder).add(landRegistryFeesPage);

        inOrder.verify(pageBuilder).add(repaymentsPage);
        inOrder.verify(pageBuilder).add(isA(LanguageUsedPage.class));
        inOrder.verify(pageBuilder).add(isA(SuspendedOrderPage.class));
        inOrder.verify(pageBuilder).add(isA(StatementOfTruthPlaceHolder.class));
        inOrder.verify(pageBuilder).add(isA(StatementOfTruthPlaceHolder2.class));

        verifyNoMoreInteractions(pageBuilder);
    }
}
