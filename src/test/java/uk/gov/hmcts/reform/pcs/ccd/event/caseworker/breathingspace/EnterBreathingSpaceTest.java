package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.breathingspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EnterBreathingSpaceTest extends BaseEventTest {

    @Mock
    private EnforcementOrderService enforcementOrderService;

    @InjectMocks
    private EnterBreathingSpace enterBreathingSpace;

    @BeforeEach
    void setUp() {
        setEventUnderTest(enterBreathingSpace);
    }


    @Test
    void shouldUpdateEnforcementAndTransitionCaseToBreathingSpace() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        verify(enforcementOrderService)
            .updateStatesForBreathingSpace(TEST_CASE_REFERENCE);

        assertThat(response.getState())
            .isEqualTo(State.BREATHING_SPACE);
    }


    @Test
    void shouldNotReturnSuccessfulSubmissionWhenEnforcementUpdateFails() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        doThrow(new IllegalStateException("Database update failed"))
            .when(enforcementOrderService)
            .updateStatesForBreathingSpace(TEST_CASE_REFERENCE);

        // When / Then
        assertThatThrownBy(() -> callSubmitHandler(pcsCase))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Database update failed");
    }


}
