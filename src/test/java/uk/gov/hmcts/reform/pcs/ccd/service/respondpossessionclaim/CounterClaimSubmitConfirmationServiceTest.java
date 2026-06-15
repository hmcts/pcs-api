package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE;

@ExtendWith(MockitoExtension.class)
class CounterClaimSubmitConfirmationServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final UUID COUNTER_CLAIM_ID = UUID.randomUUID();
    private static final String SERVICE_REQUEST_REFERENCE = "0000000000000001";
    private static final BigDecimal FEE_AMOUNT = new BigDecimal("80.00");
    private static final BigDecimal CLAIM_AMOUNT_POUNDS = new BigDecimal("2500.00");

    @Mock
    private PartyService partyService;
    @Mock
    private FeeService feeService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private CounterClaimFeeCalculator counterClaimFeeCalculator;
    @Mock
    private SecurityContextService securityContextService;

    @Captor
    private ArgumentCaptor<FeesAndPayTaskData> taskDataCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CounterClaimSubmitConfirmationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CounterClaimSubmitConfirmationService(
            partyService,
            feeService,
            paymentService,
            counterClaimFeeCalculator,
            securityContextService,
            objectMapper
        );
    }

    @Test
    void shouldReturnDefaultResponseWhenNoCounterClaimEntity() {
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder().build();
        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(possessionClaimResponse, null, false);

        SubmitResponse<State> response = underTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult);

        assertThat(response).isEqualTo(SubmitResponse.defaultResponse());
        verify(feeService, never()).getFee(any(), any());
        verify(paymentService, never()).createServiceRequest(any());
    }

    @Test
    void shouldReturnIssuedConfirmationWhenCounterClaimIssuedWithoutPayment() {
        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder().build();
        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(
                possessionClaimResponse,
                counterClaimEntity,
                true
            );

        SubmitResponse<State> response = underTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult);

        assertThat(response.getConfirmationBody())
            .contains("\"status\":\"COUNTER_CLAIM_ISSUED\"")
            .contains("\"serviceRequestReference\":null")
            .contains("\"feeAmount\":null");
        verify(feeService, never()).getFee(any(), any());
        verify(paymentService, never()).createServiceRequest(any());
    }

    @Test
    void shouldCreateServiceRequestAndReturnPaymentConfirmationWhenPaymentRequired() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(CLAIM_AMOUNT_POUNDS)
            .build();
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();
        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();
        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(
                possessionClaimResponse,
                counterClaimEntity,
                false
            );
        PartyEntity partyEntity = PartyEntity.builder().id(PARTY_ID).build();
        FeeDetails feeDetails = FeeDetails.builder()
            .code("FEE_CODE")
            .description("Counterclaim fee")
            .feeAmount(FEE_AMOUNT)
            .version(1)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(partyService.getPartyEntityByIdamId(USER_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        when(counterClaimFeeCalculator.resolveFeeType(counterClaim)).thenReturn(FeeType.COUNTER_CLAIM_RANGED);
        when(counterClaimFeeCalculator.resolveFeeLookupAmountInPounds(counterClaim))
            .thenReturn(new BigDecimal("2500.00"));
        when(feeService.getFee(FeeType.COUNTER_CLAIM_RANGED, new BigDecimal("2500.00"))).thenReturn(feeDetails);
        when(paymentService.createServiceRequest(any(FeesAndPayTaskData.class)))
            .thenReturn(PaymentServiceResponse.builder()
                .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
                .build());

        SubmitResponse<State> response = underTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult);

        verify(paymentService).createServiceRequest(taskDataCaptor.capture());
        FeesAndPayTaskData taskData = taskDataCaptor.getValue();
        assertThat(taskData.getFeeDetails()).isEqualTo(feeDetails);
        assertThat(taskData.getCcdCaseNumber()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getResponsiblePartyId()).isEqualTo(PARTY_ID);
        assertThat(taskData.getPaymentCallbackHandlerType()).isEqualTo(COUNTER_CLAIM_ISSUE);
        assertThat(taskData.getRelatedEntityId()).isEqualTo(COUNTER_CLAIM_ID);

        assertThat(response.getConfirmationBody())
            .contains("\"status\":\"PENDING_COUNTER_CLAIM_ISSUED\"")
            .contains("\"serviceRequestReference\":\"" + SERVICE_REQUEST_REFERENCE + "\"")
            .contains("\"feeAmount\":80.00");
    }

    @Test
    void shouldThrowWhenCurrentUserIdIsNullDuringPaymentCreation() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .build();
        DefendantResponses defendantResponses = DefendantResponses.builder()
            .counterClaim(counterClaim)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();
        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();
        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(
                possessionClaimResponse,
                counterClaimEntity,
                false
            );
        FeeDetails feeDetails = FeeDetails.builder().feeAmount(FEE_AMOUNT).build();

        when(counterClaimFeeCalculator.resolveFeeType(counterClaim)).thenReturn(FeeType.COUNTER_CLAIM_FLAT_FEE);
        when(counterClaimFeeCalculator.resolveFeeLookupAmountInPounds(counterClaim)).thenReturn(null);
        when(feeService.getFee(eq(FeeType.COUNTER_CLAIM_FLAT_FEE), eq(null))).thenReturn(feeDetails);
        when(securityContextService.getCurrentUserId()).thenReturn(null);

        assertThatThrownBy(() -> underTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Current user IDAM ID is null");
    }

    @Test
    void shouldThrowWhenSubmitResponseCannotBeSerialised() throws Exception {
        ObjectMapper failingObjectMapper = spy(new ObjectMapper());
        CounterClaimSubmitConfirmationService serviceUnderTest = new CounterClaimSubmitConfirmationService(
            partyService,
            feeService,
            paymentService,
            counterClaimFeeCalculator,
            securityContextService,
            failingObjectMapper
        );
        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .build();
        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(
                PossessionClaimResponse.builder().build(),
                counterClaimEntity,
                true
            );

        doThrow(new JsonProcessingException("serialisation failed") {})
            .when(failingObjectMapper).writeValueAsString(any());

        assertThatThrownBy(() -> serviceUnderTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Unable to serialise respond possession claim submit response");
    }
}
