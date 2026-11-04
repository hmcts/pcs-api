package uk.gov.hmcts.reform.pcs.enforcement.ccd.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.LanguageUsedPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.StatementOfTruthPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.LanguageUsedWritPage;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.writ.StatementOfTruthWritPage;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.PageConfigurerHelper.verifyAndCount;

@ExtendWith(MockitoExtension.class)
class EnforcementDeclarationPageConfigurerTest extends BasePageTest {

    @InjectMocks
    private EnforcementDeclarationPageConfigurer enforcementDeclarationPageConfigurer;

    @Test
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        SavingPageBuilder pageBuilder = mock(SavingPageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        enforcementDeclarationPageConfigurer.configurePages(pageBuilder);

        // Then
        ArgumentCaptor<CcdPageConfiguration> pageCaptor = ArgumentCaptor.forClass(CcdPageConfiguration.class);
        InOrder inOrder = Mockito.inOrder(pageBuilder);
        Mockito.verify(pageBuilder, Mockito.atLeastOnce()).add(pageCaptor.capture());
        AtomicInteger verificationCount = new AtomicInteger(0);

        verifyAndCount(inOrder, pageBuilder, LanguageUsedPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, StatementOfTruthPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, LanguageUsedWritPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, StatementOfTruthWritPage.class, verificationCount);

        int numberOfPages = pageCaptor.getAllValues().size();
        assertThat(verificationCount.get()).isEqualTo(numberOfPages);

        verifyNoMoreInteractions(pageBuilder);
    }
}
