package uk.gov.hmcts.reform.pcs.ccd.page.addcasereviewdate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddCaseReviewDateConfigurerTest extends BasePageTest {

    @Mock
    private AddCaseReviewDatePage addCaseReviewDatePage;

    @InjectMocks
    private AddCaseReviewDateConfigurer reviewDateConfigurer;

    @Test
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        PageBuilder pageBuilder = mock(PageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        reviewDateConfigurer.configurePages(pageBuilder);

        // Then
        verify(pageBuilder, times(1)).add(any());
        verify(pageBuilder).add(addCaseReviewDatePage);
    }
}
