package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestUpdate;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentCallBackControllerTest {

    private PaymentCallBackController underTest;

    @Mock
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        underTest = new PaymentCallBackController(paymentService);
    }

    @Test
    void shouldProcessPaymentCallback() {
        // Given
        ServiceRequestUpdate serviceRequestUpdate = ServiceRequestUpdate.builder()
            .serviceRequestReference("SR-123")
            .ccdCaseNumber("1234123412341234")
            .build();

        // When
        underTest.processPaymentCallback("auth", "s2s", serviceRequestUpdate.toString());

        // Then

    }
}
