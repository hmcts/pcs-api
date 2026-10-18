package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;

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
    private DefendantsDOBPage defendantsDOBPage;
    @Mock
    private WarrantRiskPagesConfigurer warrantRiskPagesConfigurer;

    @Test
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        SavingPageBuilder pageBuilder = mock(SavingPageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        warrantPageConfigurer.configurePages(pageBuilder);

        // Then
        InOrder inOrder = inOrder(pageBuilder, warrantRiskPagesConfigurer);
        inOrder.verify(pageBuilder).add(isA(BreathingSpacePage.class));
        inOrder.verify(pageBuilder).add(isA(MissedPaymentsPage.class));
        inOrder.verify(pageBuilder).add(isA(CannotApplyForWarrantInfoPage.class));
        inOrder.verify(pageBuilder).add(isA(NameAndAddressForEvictionPage.class));
        inOrder.verify(pageBuilder).add(isA(ChangeNameAddressPage.class));
        inOrder.verify(pageBuilder).add(isA(ConfirmIfDOBKnownPage.class));
        inOrder.verify(pageBuilder).add(defendantsDOBPage);
        inOrder.verify(warrantRiskPagesConfigurer).configurePages(pageBuilder);
        inOrder.verify(pageBuilder).add(isA(LanguageUsedPage.class));
        inOrder.verify(pageBuilder).add(isA(StatementOfTruthPage.class));
        verifyNoMoreInteractions(pageBuilder, warrantRiskPagesConfigurer);
    }
}
