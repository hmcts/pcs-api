package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.addjudicialnote.AddJudicialNotePage;
import uk.gov.hmcts.reform.pcs.ccd.service.JudicialNoteService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddJudicialNoteTest extends BaseEventTest {

    @Mock
    private AddJudicialNotePage addJudicialNotePage;

    @Mock
    private JudicialNoteService judicialNoteService;

    @InjectMocks
    private AddJudicialNote addJudicialNote;

    @BeforeEach
    void setUp() {
        setEventUnderTest(addJudicialNote);
    }

    @Test
    void shouldCallJudicialNoteServiceOnSubmit() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        verify(judicialNoteService).addJudicialNote(TEST_CASE_REFERENCE, pcsCase);
        assertThat(response.getConfirmationBody()).contains("Judicial note added");
    }

}
