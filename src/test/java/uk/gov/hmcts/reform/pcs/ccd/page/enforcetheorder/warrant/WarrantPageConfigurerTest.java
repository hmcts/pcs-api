package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantPageConfigurerTest {

    @InjectMocks
    private WarrantPageConfigurer warrantPageConfigurer;

    @Test
    void shouldConfigureAllPages() {
        // Given
        SavingPageBuilder pageBuilder = mock(SavingPageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        warrantPageConfigurer.configurePages(pageBuilder);

        // Then
        verify(pageBuilder, times(26)).add(any());
    }
}