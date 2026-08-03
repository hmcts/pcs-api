package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.addcasenote.AddCaseNoteConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNoteService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddCaseNoteTest extends BaseEventTest {

    @Mock
    private CaseNoteService caseNoteService;

    @Mock
    private AddCaseNoteConfigurer addCaseNoteConfigurer;

    @InjectMocks
    private AddCaseNote addCaseNote;

    @BeforeEach
    void setUp() {
        setEventUnderTest(addCaseNote);
    }

    @Test
    void shouldConfigurePages() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(addCaseNoteConfigurer).configurePages(any(PageBuilder.class));
    }

    @Test
    void shouldCallCaseNoteServiceOnSubmit() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(caseNoteService).addCaseNote(TEST_CASE_REFERENCE, pcsCase);
    }
}
