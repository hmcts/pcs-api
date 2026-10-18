package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant.WarrantRiskPagesConfigurer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmEvictionPageConfigurerTest extends BasePageTest {

    @Mock
    private WarrantRiskPagesConfigurer warrantRiskPagesConfigurer;

    @InjectMocks
    private ConfirmEvictionConfigurer underTest;

    @Test
    void shouldAskAboutAttendanceThenTheWarrantRiskQuestions() {
        // Given
        PageBuilder pageBuilder = mock(PageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        underTest.configurePages(pageBuilder);

        // Then
        InOrder inOrder = inOrder(pageBuilder, warrantRiskPagesConfigurer);
        inOrder.verify(pageBuilder).add(isA(ConfirmEvictionDetailsPage.class));
        inOrder.verify(pageBuilder).add(isA(EvictionDatePage.class));
        inOrder.verify(warrantRiskPagesConfigurer).configurePages(pageBuilder);
        verifyNoMoreInteractions(pageBuilder, warrantRiskPagesConfigurer);
    }
}
