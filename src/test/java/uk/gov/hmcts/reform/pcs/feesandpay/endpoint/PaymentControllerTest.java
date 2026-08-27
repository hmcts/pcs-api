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
import uk.gov.hmcts.reform.pcs.feesandpay.model.PbaAccountsResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PbaPaymentRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PbaPaymentResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private static final String S2S_TOKEN = "some s2s token";
    private static final String AUTH_TOKEN = "some auth token";

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
        String internalPaymentReference = UUID.randomUUID().toString();

        when(paymentService.getPaymentStatus(internalPaymentReference))
            .thenReturn(paymentStatusResponse);

        // When
        ResponseEntity<CardPaymentStatusResponse> response = underTest.getCardPaymentStatus(
            S2S_TOKEN,
            internalPaymentReference
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(paymentStatusResponse);
    }

    @Test
    void shouldDelegateRequestToGetPbaAccounts() {
        // Given
        PbaAccountsResponse pbaAccountsResponse = PbaAccountsResponse.builder()
            .pbaAccounts(List.of("PBA1234567"))
            .build();

        when(paymentService.getPbaAccounts(AUTH_TOKEN))
            .thenReturn(pbaAccountsResponse);

        // When
        ResponseEntity<PbaAccountsResponse> response = underTest.getPbaAccounts(
            AUTH_TOKEN,
            S2S_TOKEN
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pbaAccountsResponse);
        verify(paymentService).getPbaAccounts(AUTH_TOKEN);
    }

    @Test
    void shouldDelegateRequestToCreatePbaPaymentRequest() {
        // Given
        String serviceRequestReference = "ABC-123";
        PbaPaymentRequest pbaPaymentRequest = PbaPaymentRequest.builder()
            .amount(BigDecimal.valueOf(300.99))
            .pbaAccount("PBA1234567")
            .customerReference("customer-reference")
            .build();
        PbaPaymentResponse pbaServiceRequestResponse = mock(PbaPaymentResponse.class);

        when(paymentService.createPbaPaymentRequest(AUTH_TOKEN, serviceRequestReference, pbaPaymentRequest))
            .thenReturn(pbaServiceRequestResponse);

        // When
        ResponseEntity<PbaPaymentResponse> response = underTest.createPbaPaymentRequest(
            S2S_TOKEN,
            AUTH_TOKEN,
            serviceRequestReference,
            pbaPaymentRequest
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(pbaServiceRequestResponse);
        verify(paymentService).createPbaPaymentRequest(AUTH_TOKEN, serviceRequestReference, pbaPaymentRequest);
    }

}
