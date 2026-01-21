package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementApplicationPage;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.PageConfigurerHelper.verifyAndCount;

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
    private DefendantsDOBPage defendantsDOBPage;

    @Test
    @SuppressWarnings("squid:S5961")
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        SavingPageBuilder pageBuilder = mock(SavingPageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        warrantPageConfigurer.configurePages(pageBuilder);

        // Then
        ArgumentCaptor<CcdPageConfiguration> pageCaptor = ArgumentCaptor.forClass(CcdPageConfiguration.class);
        InOrder inOrder = inOrder(pageBuilder);
        Mockito.verify(pageBuilder, Mockito.atLeastOnce()).add(pageCaptor.capture());
        AtomicInteger verificationCount = new AtomicInteger(0);

        verifyAndCount(inOrder, pageBuilder, EnforcementApplicationPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, NameAndAddressForEvictionPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, ChangeNameAddressPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, ConfirmIfDOBKnownPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, defendantsDOBPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, PeopleWhoWillBeEvictedPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, PeopleYouWantToEvictPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, LivingInThePropertyPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, EvictionDelayWarningPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, EvictionRisksPosedPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, violentAggressiveRiskPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, firearmsPossessionRiskPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, criminalAntisocialRiskPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, verbalOrWrittenThreatsRiskPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, protestorGroupRiskPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, policeOrSocialServicesRiskPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, aggressiveAnimalsRiskPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, vulnerableAdultsChildrenPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, propertyAccessDetailsPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, additionalInformationPage, verificationCount);

        verifyAndCount(inOrder, pageBuilder, MoneyOwedPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, LegalCostsPage.class, verificationCount);

        verifyAndCount(inOrder, pageBuilder, landRegistryFeesPage, verificationCount);

        verifyAndCount(inOrder, pageBuilder, RepaymentsPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, LanguageUsedPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, SuspendedOrderPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, StatementOfTruthPlaceHolder.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, StatementOfTruthPlaceHolder2.class, verificationCount);

        int numberOfPages = pageCaptor.getAllValues().size();
        assertThat(verificationCount.get()).isEqualTo(numberOfPages);

        verifyNoMoreInteractions(pageBuilder);
    }
}
