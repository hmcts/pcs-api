package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.bailiffnote;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddBailiffNoteConfigurerTest {

    @Test
    void shouldAddTheNotePage() {
        PageBuilder pageBuilder = mock(PageBuilder.class);
        when(pageBuilder.add(any())).thenReturn(pageBuilder);

        new AddBailiffNoteConfigurer().configurePages(pageBuilder);

        verify(pageBuilder).add(isA(AddBailiffNotePage.class));
    }
}
