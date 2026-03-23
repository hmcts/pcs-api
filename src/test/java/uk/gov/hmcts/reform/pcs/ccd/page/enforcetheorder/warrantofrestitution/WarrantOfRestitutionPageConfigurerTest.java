package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

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
class WarrantOfRestitutionPageConfigurerTest extends BasePageTest {

    @Mock
    private ExplainHowDefendantsReturnedPage explainHowDefendantsReturnedPage;

    @Mock
    private VulnerableAdultsChildrenWarrantRestPage vulnerableAdultsChildrenWarrantRestPage;

    @InjectMocks
    private WarrantOfRestitutionPageConfigurer warrantOfRestitutionPageConfigurer;

    @Mock
    private PropertyAccessDetailsWarrantOfRestitutionPage propertyAccessDetailsWarrantOfRestitutionPage;

    @Test
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        SavingPageBuilder pageBuilder = mock(SavingPageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        warrantOfRestitutionPageConfigurer.configurePages(pageBuilder);

        // Then
        ArgumentCaptor<CcdPageConfiguration> pageCaptor = ArgumentCaptor.forClass(CcdPageConfiguration.class);
        InOrder inOrder = Mockito.inOrder(pageBuilder);
        Mockito.verify(pageBuilder, Mockito.atLeastOnce()).add(pageCaptor.capture());
        AtomicInteger verificationCount = new AtomicInteger(0);
        verifyAndCount(inOrder, pageBuilder, PeopleWhoWillBeEvictedWarrantRestPlaceholder.class,
                verificationCount);
        verifyAndCount(inOrder, pageBuilder, ShareEvidenceWithJudgePage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, explainHowDefendantsReturnedPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, DefendantAtPropertyPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, LivingInThePropertyIntroPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, LivingInThePropertyPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, EvictionDelayWarningPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, EvictionRisksPosedPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, vulnerableAdultsChildrenWarrantRestPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, propertyAccessDetailsWarrantOfRestitutionPage, verificationCount);
        verifyAndCount(inOrder, pageBuilder, AnythingElseToHelpTheEvictionPlaceholder.class,
                       verificationCount);

        int numberOfPages = pageCaptor.getAllValues().size();
        assertThat(verificationCount.get()).isEqualTo(numberOfPages);

        verifyNoMoreInteractions(pageBuilder);
    }
}
