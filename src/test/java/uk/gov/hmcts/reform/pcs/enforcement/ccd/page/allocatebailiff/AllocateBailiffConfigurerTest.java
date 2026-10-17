package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.allocatebailiff;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffDirectoryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AllocateBailiffConfigurerTest {

    @Test
    void shouldAddOnlyTheFindBailiffPage() {
        PageBuilder pageBuilder = mock(PageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        new AllocateBailiffConfigurer(mock(BailiffDirectoryService.class)).configurePages(pageBuilder);

        verify(pageBuilder).add(isA(FindBailiffPage.class));
        verifyNoMoreInteractions(pageBuilder);
    }
}
