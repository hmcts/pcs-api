package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.ServiceRequestUpdate;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCallBackControllerTest {

    private PaymentCallBackController underTest;

    @Mock
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        underTest = new PaymentCallBackController(objectMapper, paymentService);
    }

    @Test
    void shouldProcessPaymentCallback() throws Exception {
        // Given
        String ccdCaseNumber = "123123123123";
        BigDecimal amount = new BigDecimal("123.11");
        ServiceRequestUpdate serviceRequestUpdate = ServiceRequestUpdate.builder()
            .serviceRequestReference("SR-123")
            .ccdCaseNumber(ccdCaseNumber)
            .serviceRequestAmount(amount)
            .serviceRequestStatus("Paid")
            .payment(Payment.builder()
                         .paymentReference("123")
                         .paymentAmount(amount)
                         .paymentMethod("Card")
                         .accountNumber("123123123")
                         .caseReference(ccdCaseNumber)
                         .build())
            .build();

        String requestBody = objectMapper.writeValueAsString(serviceRequestUpdate);

        // When
        underTest.processPaymentCallback("s2s", requestBody);

        // Then
        verify(paymentService).processPaymentResponse(any(ServiceRequestUpdate.class));
    }

    @Test
    void shouldProcessRequestBody() throws Exception {
        // Given
        String ccdCaseNumber = "123123123123";
        BigDecimal amount = new BigDecimal("123.11");
        ServiceRequestUpdate serviceRequestUpdate = ServiceRequestUpdate.builder()
            .serviceRequestReference("SR-123")
            .ccdCaseNumber(ccdCaseNumber)
            .serviceRequestAmount(amount)
            .serviceRequestStatus("Paid")
            .payment(Payment.builder()
                         .paymentReference("123")
                         .paymentAmount(amount)
                         .paymentMethod("Card")
                         .accountNumber("123123123")
                         .caseReference(ccdCaseNumber)
                         .build())
            .build();

        String requestBody = objectMapper.writeValueAsString(serviceRequestUpdate);

        // When
        underTest.processRequestBody(requestBody);

        // Then
        verify(paymentService).processPaymentResponse(any(ServiceRequestUpdate.class));
    }

    @Test
    void shouldNotProcessPaymentCallbackWhenRequestBodyIsInvalidJson() {
        // Given
        String invalidRequestBody = "{ test";

        // When // Then
        assertThatThrownBy(() -> underTest.processPaymentCallback("s2s", invalidRequestBody))
            .isInstanceOf(JsonProcessingException.class);
    }

}
