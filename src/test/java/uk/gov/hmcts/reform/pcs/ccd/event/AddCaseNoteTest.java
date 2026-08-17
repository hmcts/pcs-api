package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.addcasenote.AddCaseNoteConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNoteService;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@ExtendWith(MockitoExtension.class)
public class AddCaseNoteTest extends BaseEventTest {

    @Mock
    private CaseNoteService caseNoteService;

    @Mock
    private AddCaseNoteConfigurer addCaseNoteConfigurer;
    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private AddCaseNote addCaseNote;

    @BeforeEach
    void setUp() {
        when(featureToggleService.isEnabled(RELEASE_1_DOT_3)).thenReturn(true);
        setEventUnderTest(addCaseNote);
    }

    @Test
    void shouldBeConfiguredForEventStates() {
        assertConfiguredForStates(EventStates.addCaseNote());
    }

    @Test
    void shouldBeConfiguredForPreRelease1dot3EventStatesWhenFeatureFlagDisabled() {
        when(featureToggleService.isEnabled(RELEASE_1_DOT_3)).thenReturn(false);
        setEventUnderTest(addCaseNote);

        assertConfiguredForStates(State.PENDING_CASE_ISSUED, State.CASE_ISSUED);
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
