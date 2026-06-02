package uk.gov.hmcts.reform.pcs.feesandpay.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CardPaymentStatusResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.CreateCardPaymentResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private static final String AUTH_TOKEN = "some auth token";
    private static final String S2S_TOKEN = "some s2s token";
    @Mock
    private PaymentService paymentService;

    private PaymentController underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentController(paymentService);
    }

    @Test
    void shouldDelegateRequestToCreatePaymentRequest() {
        // Given
        CreateCardPaymentRequest cardPaymentRequest = mock(CreateCardPaymentRequest.class);
        CreateCardPaymentResponse createCardPaymentResponse = mock(CreateCardPaymentResponse.class);
        String serviceRequestReference = "ABC-123";

        when(paymentService.createPaymentRequest(serviceRequestReference, cardPaymentRequest))
            .thenReturn(createCardPaymentResponse);

        // When
        ResponseEntity<CreateCardPaymentResponse> response = underTest.createPaymentRequest(
            AUTH_TOKEN,
            S2S_TOKEN,
            serviceRequestReference,
            cardPaymentRequest
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(createCardPaymentResponse);
    }

    @Test
    void shouldDelegateRequestToGetCardPaymentStatus() {
        // Given
        CardPaymentStatusResponse paymentStatusResponse = mock(CardPaymentStatusResponse.class);
        String paymentReference = "CP-123";

        when(paymentService.getPaymentStatus(paymentReference))
            .thenReturn(paymentStatusResponse);

        // When
        ResponseEntity<CardPaymentStatusResponse> response = underTest.getCardPaymentStatus(
            AUTH_TOKEN,
            S2S_TOKEN,
            paymentReference
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(paymentStatusResponse);
    }

}
