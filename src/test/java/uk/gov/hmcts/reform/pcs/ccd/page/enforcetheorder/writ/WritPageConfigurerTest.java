package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

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

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.PageConfigurerHelper.verifyAndCount;

@ExtendWith(MockitoExtension.class)
class WritPageConfigurerTest extends BasePageTest {

    @InjectMocks
    private WritPageConfigurer writPageConfigurer;

    @Mock
    private HCEOfficerDetailsPage hceOfficerDetailsPage;

    @Mock
    private LandRegistryFeesWritPage landRegistryFeesWritPage;

    @Test
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        SavingPageBuilder pageBuilder = mock(SavingPageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        writPageConfigurer.configurePages(pageBuilder);

        // Then
        ArgumentCaptor<CcdPageConfiguration> pageCaptor = ArgumentCaptor.forClass(CcdPageConfiguration.class);
        InOrder inOrder = Mockito.inOrder(pageBuilder);
        Mockito.verify(pageBuilder, Mockito.atLeastOnce()).add(pageCaptor.capture());
        AtomicInteger verificationCount = new AtomicInteger(0);

        verifyAndCount(inOrder, pageBuilder, NameAndAddressForEvictionWritPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, ChangeNameAddressWritPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, ConfirmHCEOfficerPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, hceOfficerDetailsPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, EnforcementOfficerSelectionPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, MoneyOwedWritPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, LegalCostsWritPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, landRegistryFeesWritPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, RepaymentsWritPage.class, verificationCount);

        int numberOfPages = pageCaptor.getAllValues().size();
        assertThat(verificationCount.get()).isEqualTo(numberOfPages);

        verifyNoMoreInteractions(pageBuilder);
    }
}
