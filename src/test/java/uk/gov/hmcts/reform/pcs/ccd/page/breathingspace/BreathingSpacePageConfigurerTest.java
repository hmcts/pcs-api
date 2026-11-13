package uk.gov.hmcts.reform.pcs.ccd.page.breathingspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.PageConfigurerHelper.verifyAndCount;

@ExtendWith(MockitoExtension.class)
class BreathingSpacePageConfigurerTest extends BasePageTest {

    @InjectMocks
    private BreathingSpacePageConfigurer underTest;

    @Mock
    private BreathingSpaceApplicationPage breathingSpaceApplicationPage;

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
        verify(pageBuilder, atLeastOnce()).add(pageCaptor.capture());
        AtomicInteger verificationCount = new AtomicInteger(0);

        verifyAndCount(inOrder, pageBuilder, breathingSpaceApplicationPage, verificationCount);

        int numberOfPages = pageCaptor.getAllValues().size();
        assertThat(verificationCount.get()).isEqualTo(numberOfPages);

        verifyNoMoreInteractions(pageBuilder);
    }
}
