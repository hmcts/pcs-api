package uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.PageConfigurerHelper.verifyAndCount;

@ExtendWith(MockitoExtension.class)
class LegalRepDocumentUploadDetailsConfigurerTest extends BasePageTest {

    @InjectMocks
    private LegalRepDocumentUploadConfigurer underTest;

    @Mock
    private UploadAdditionalDocumentsPage uploadAdditionalDocumentsPage;

    @Test
    @SuppressWarnings("squid:S5961")
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        PageBuilder pageBuilder = mock(PageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        underTest.configurePages(pageBuilder);

        // Then
        ArgumentCaptor<CcdPageConfiguration> pageCaptor = ArgumentCaptor.forClass(CcdPageConfiguration.class);
        InOrder inOrder = inOrder(pageBuilder);
        Mockito.verify(pageBuilder, Mockito.atLeastOnce()).add(pageCaptor.capture());
        AtomicInteger verificationCount = new AtomicInteger(0);

        verifyAndCount(inOrder, pageBuilder, UploadAdditionalDocumentsInformationPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, ExistingApplicationPage.class, verificationCount);
        verifyAndCount(inOrder, pageBuilder, uploadAdditionalDocumentsPage, verificationCount);

        int numberOfPages = pageCaptor.getAllValues().size();
        assertThat(verificationCount.get()).isEqualTo(numberOfPages);

        verifyNoMoreInteractions(pageBuilder);
    }
}
