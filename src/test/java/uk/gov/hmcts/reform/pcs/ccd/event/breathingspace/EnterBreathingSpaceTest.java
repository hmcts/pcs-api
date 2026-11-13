package uk.gov.hmcts.reform.pcs.ccd.event.breathingspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.breathingspace.BreathingSpacePageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.breathingspace.BreathingSpaceService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class EnterBreathingSpaceTest extends BaseEventTest {

    @Mock
    private BreathingSpacePageConfigurer breathingSpacePageConfigurer;
    @Mock
    private BreathingSpaceService breathingSpaceService;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private EnforcementOrderService enforcementOrderService;

    @InjectMocks
    private EnterBreathingSpace enterBreathingSpace;

    @BeforeEach
    void setUp() {
        setEventUnderTest(enterBreathingSpace);
    }

    @Test
    void shouldConfigurePages() {
        verify(breathingSpacePageConfigurer).configurePages(any(PageBuilder.class));
    }

    @Test
    void shouldCreateBreathingSpaceOnSubmit() {
        // Given
        BreathingSpaceDetails details = BreathingSpaceDetails.builder()
            .referenceNumber("123")
            .startDate(LocalDate.of(2026, 1, 1))
            .type(BreathingSpaceType.STANDARD)
            .expectedEndDate(LocalDate.of(2027, 1, 1))
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .breathingSpaceDetails(details)
            .build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(breathingSpaceService).createBreathingSpace(TEST_CASE_REFERENCE, details);
        verify(enforcementOrderService).updateStatesForBreathingSpace(TEST_CASE_REFERENCE);
    }

    @Test
    void shouldReturnConfirmationBodyWithCaseDetails() {
        // Given
        AddressUK address = AddressUK.builder().build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(address)
            .caseNameHmctsInternal("Claimant v Defendant")
            .build();

        when(addressFormatter.formatMediumAddress(address, COMMA_DELIMITER)).thenReturn("1 Test Street, London");

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Breathing space has been entered")
            .contains("Case number #" + TEST_CASE_REFERENCE)
            .contains("1 Test Street, London")
            .contains("Claimant v Defendant");
    }


    @Test
    void shouldTransitionCaseToBreathingSpaceOnSubmit() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .breathingSpaceDetails(BreathingSpaceDetails.builder().build())
            .build();

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        assertThat(response.getState())
            .isEqualTo(State.BREATHING_SPACE);
    }


}
