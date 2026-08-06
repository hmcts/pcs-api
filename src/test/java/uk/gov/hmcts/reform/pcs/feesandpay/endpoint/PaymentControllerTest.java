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
import uk.gov.hmcts.reform.pcs.feesandpay.model.OutstandingCounterClaimPayment;
import uk.gov.hmcts.reform.pcs.feesandpay.service.OutstandingCounterClaimPaymentService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private static final String S2S_TOKEN = "some s2s token";
    private static final String AUTHORIZATION = "Bearer token";

    @Mock
    private PaymentService paymentService;

    @Mock
    private OutstandingCounterClaimPaymentService outstandingCounterClaimPaymentService;

    @Mock
    private IdamAuthenticator idamAuthenticator;

    private PaymentController underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentController(
            paymentService,
            outstandingCounterClaimPaymentService,
            idamAuthenticator
        );
    }

    @Test
    void shouldDelegateRequestToCreatePaymentRequest() {
        CreateCardPaymentRequest cardPaymentRequest = mock(CreateCardPaymentRequest.class);
        CreateCardPaymentResponse createCardPaymentResponse = mock(CreateCardPaymentResponse.class);
        String serviceRequestReference = "ABC-123";

        when(paymentService.createPaymentRequest(serviceRequestReference, cardPaymentRequest))
            .thenReturn(createCardPaymentResponse);

        ResponseEntity<CreateCardPaymentResponse> response = underTest.createPaymentRequest(
            S2S_TOKEN,
            serviceRequestReference,
            cardPaymentRequest
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(createCardPaymentResponse);
    }

    @Test
    void shouldDelegateRequestToGetCardPaymentStatus() {
        CardPaymentStatusResponse paymentStatusResponse = mock(CardPaymentStatusResponse.class);
        String internalPaymentReference = UUID.randomUUID().toString();

        when(paymentService.getPaymentStatus(internalPaymentReference))
            .thenReturn(paymentStatusResponse);

        ResponseEntity<CardPaymentStatusResponse> response = underTest.getCardPaymentStatus(
            S2S_TOKEN,
            internalPaymentReference
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(paymentStatusResponse);
    }

    @Test
    void shouldReturnOutstandingCounterClaimPaymentForAuthenticatedDefendant() {
        long caseReference = 12_345_678L;
        UUID idamUserId = UUID.randomUUID();
        OutstandingCounterClaimPayment outstandingPayment = OutstandingCounterClaimPayment.builder()
            .serviceRequestReference("2026-1234567890123")
            .feeAmount(new BigDecimal("404.00"))
            .build();

        UserInfo userDetails = mock(UserInfo.class);
        when(userDetails.getUid()).thenReturn(idamUserId.toString());
        User user = mock(User.class);
        when(user.getUserDetails()).thenReturn(userDetails);
        when(idamAuthenticator.validateAuthToken(AUTHORIZATION)).thenReturn(user);
        when(outstandingCounterClaimPaymentService.getOutstandingForDefendant(caseReference, idamUserId))
            .thenReturn(outstandingPayment);

        ResponseEntity<OutstandingCounterClaimPayment> response = underTest.getOutstandingCounterClaimPayment(
            AUTHORIZATION,
            S2S_TOKEN,
            caseReference
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(outstandingPayment);
    }

}
