package uk.gov.hmcts.reform.pcs.feesandpay.event;

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
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClaimIssuePaymentTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private ClaimIssuePayment paymentEvent;

    @BeforeEach
    void setUp() {
        setEventUnderTest(paymentEvent);
    }

    @Test
    void shouldTransitionToCaseIssued() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getState()).isEqualTo(State.CASE_ISSUED);
    }

    @Test
    void shouldSetCaseIssuedDateOnSubmitWhenDateIssuedNotSet() {
        callSubmitHandler(PCSCase.builder().build());

        verify(pcsCaseService).setCaseIssuedDate(TEST_CASE_REFERENCE);
    }

    @Test
    void shouldNotSetCaseIssuedDateWhenDateIssuedAlreadySet() {
        PCSCase pcsCase = PCSCase.builder()
            .dateIssued(LocalDateTime.of(2026, 1, 1, 9, 0, 0))
            .build();

        callSubmitHandler(pcsCase);

        verify(pcsCaseService, never()).setCaseIssuedDate(TEST_CASE_REFERENCE);
    }
}
