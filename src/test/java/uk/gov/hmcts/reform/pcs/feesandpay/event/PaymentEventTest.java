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

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PaymentEventTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private PaymentEvent paymentEvent;

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
}
