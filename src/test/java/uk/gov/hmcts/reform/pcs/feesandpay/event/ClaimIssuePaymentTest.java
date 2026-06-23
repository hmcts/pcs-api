package uk.gov.hmcts.reform.pcs.feesandpay.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.time.LocalDateTime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ClaimIssuePaymentTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private ClaimIssuePayment paymentEvent;

    @BeforeEach
    void setUp() {
        setEventUnderTest(paymentEvent);
    }

    @Test
    void shouldCallPcsCaseServiceOnSubmit() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseService).setCaseIssuedDate(1234L);
    }

    @Test
    void shouldNotCallPcsCaseServiceOnSubmitWhenDateIssuedSet() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .dateIssued(LocalDateTime.of(2026, 1, 1, 9, 0, 0))
            .build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseService, never()).setCaseIssuedDate(1234L);
    }
}
