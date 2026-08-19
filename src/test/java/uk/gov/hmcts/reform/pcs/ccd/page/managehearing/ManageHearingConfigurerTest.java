package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

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
public class ManageHearingConfigurerTest extends BasePageTest {

    @Mock
    private ManageHearingPage manageHearingPage;

    @Mock
    private HearingDetailsPage hearingDetailsPage;

    @Mock
    private CancelHearingPage cancelHearingPage;

    @InjectMocks
    private ManageHearingConfigurer manageHearingConfigurer;

    @Test
    void shouldConfigurePagesInCorrectOrder() {
        // Given
        PageBuilder pageBuilder = mock(PageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        // When
        manageHearingConfigurer.configurePages(pageBuilder);
        verify(pageBuilder, times(3)).add(any());
        verify(pageBuilder).add(manageHearingPage);
        verify(pageBuilder).add(hearingDetailsPage);
        verify(pageBuilder).add(cancelHearingPage);
    }
}
