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
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddJudicialNoteTest extends BaseEventTest {

    @Mock
    private AddJudicialNotePage addJudicialNotePage;
    @Mock
    private JudicialNoteService judicialNoteService;
    @Mock
    private ConfirmationPanelBuilder confirmationPanelBuilder;

    @InjectMocks
    private AddJudicialNote addJudicialNote;

    @BeforeEach
    void setUp() {
        setEventUnderTest(addJudicialNote);
    }

    @Test
    void shouldCallJudicialNoteServiceOnSubmit() {
        // Given
        String confirmationPanelMarkup = stubConfirmationPanelBuilder();

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        verify(judicialNoteService).addJudicialNote(TEST_CASE_REFERENCE, pcsCase);
        verify(confirmationPanelBuilder)
            .buildConfirmationPanel("Judicial note added", List.of("You do not need to do anything else"));
        assertThat(response.getConfirmationBody()).contains(confirmationPanelMarkup);
    }

    @Test
    void shouldShowEventWithoutAFeatureFlagCondition() {
        assertConfiguredShowConditions(null);
    }

    private String stubConfirmationPanelBuilder() {
        String confirmationPanelMarkup = "confirmation panel markup";
        when(confirmationPanelBuilder.buildConfirmationPanel(anyString(), anyList()))
            .thenReturn(confirmationPanelMarkup);

        return confirmationPanelMarkup;
    }

}
