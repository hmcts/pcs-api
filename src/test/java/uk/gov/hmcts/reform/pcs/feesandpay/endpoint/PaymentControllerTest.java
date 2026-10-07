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
import uk.gov.hmcts.reform.pcs.feesandpay.model.PbaAccountsResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PbaPaymentRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PbaPaymentResponse;
import uk.gov.hmcts.reform.pcs.feesandpay.service.OutstandingCounterClaimPaymentService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private static final String S2S_TOKEN = "some s2s token";
    private static final String AUTHORIZATION = "Bearer token";
    private static final String AUTH_TOKEN = "some auth token";

    @Mock
    private PaymentService paymentService;

    @Mock
    private OutstandingCounterClaimPaymentService outstandingCounterClaimPaymentService;

    @Mock
    private IdamAuthenticator idamAuthenticator;

    @Mock
    private FeatureToggleService featureToggleService;

    private PaymentController underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentController(
            paymentService,
            outstandingCounterClaimPaymentService,
            idamAuthenticator,
            featureToggleService
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
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);
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

    @Test
    void shouldReturnNotFoundWhenRelease12DisabledForOutstandingCounterClaimPayment() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(false);

        ResponseEntity<OutstandingCounterClaimPayment> response = underTest.getOutstandingCounterClaimPayment(
            AUTHORIZATION,
            S2S_TOKEN,
            12_345_678L
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verifyNoInteractions(idamAuthenticator, outstandingCounterClaimPaymentService);
    }

    @Test
    void shouldDelegateRequestToGetPbaAccounts() {
        PbaAccountsResponse pbaAccountsResponse = PbaAccountsResponse.builder()
            .pbaAccounts(List.of("PBA1234567"))
            .build();

        when(paymentService.getPbaAccounts(AUTH_TOKEN))
            .thenReturn(pbaAccountsResponse);

        ResponseEntity<PbaAccountsResponse> response = underTest.getPbaAccounts(
            AUTH_TOKEN,
            S2S_TOKEN
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pbaAccountsResponse);
        verify(paymentService).getPbaAccounts(AUTH_TOKEN);
    }

    @Test
    void shouldDelegateRequestToCreatePbaPaymentRequest() {
        String serviceRequestReference = "ABC-123";
        PbaPaymentRequest pbaPaymentRequest = PbaPaymentRequest.builder()
            .amount(BigDecimal.valueOf(300.99))
            .pbaAccount("PBA1234567")
            .customerReference("customer-reference")
            .build();
        PbaPaymentResponse pbaServiceRequestResponse = mock(PbaPaymentResponse.class);

        when(paymentService.createPbaPaymentRequest(AUTH_TOKEN, serviceRequestReference, pbaPaymentRequest))
            .thenReturn(pbaServiceRequestResponse);

        ResponseEntity<PbaPaymentResponse> response = underTest.createPbaPaymentRequest(
            S2S_TOKEN,
            AUTH_TOKEN,
            serviceRequestReference,
            pbaPaymentRequest
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(pbaServiceRequestResponse);
        verify(paymentService).createPbaPaymentRequest(AUTH_TOKEN, serviceRequestReference, pbaPaymentRequest);
    }

}
