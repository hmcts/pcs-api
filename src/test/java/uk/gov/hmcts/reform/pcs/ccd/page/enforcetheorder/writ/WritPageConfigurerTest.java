package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritPageConfigurerTest extends BasePageTest {

    @InjectMocks
    private WritPageConfigurer writPageConfigurer;

    @Test
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        SavingPageBuilder pageBuilder = mock(SavingPageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        writPageConfigurer.configurePages(pageBuilder);
        InOrder inOrder = Mockito.inOrder(pageBuilder);

        // Then
        inOrder.verify(pageBuilder).add(isA(NameAndAddressForEvictionWritPage.class));
        inOrder.verify(pageBuilder).add(isA(ChangeNameAddressWritPage.class));
        inOrder.verify(pageBuilder).add(isA(ConfirmHiringEnforcementOfficerPlaceholder.class));

        verifyNoMoreInteractions(pageBuilder);
    }
}
