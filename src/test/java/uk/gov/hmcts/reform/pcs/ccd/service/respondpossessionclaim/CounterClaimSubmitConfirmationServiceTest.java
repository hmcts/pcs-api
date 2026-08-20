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
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
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
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE;

@ExtendWith(MockitoExtension.class)
class CounterClaimSubmitConfirmationServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final UUID COUNTER_CLAIM_ID = UUID.randomUUID();
    private static final String SERVICE_REQUEST_REFERENCE = "0000000000000001";
    private static final BigDecimal FEE_AMOUNT = new BigDecimal("80.00");
    private static final BigDecimal CLAIM_AMOUNT_POUNDS = new BigDecimal("2500.00");

    @Mock
    private PartyService partyService;
    @Mock
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<FeesAndPayTaskData> taskDataCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CounterClaimSubmitConfirmationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CounterClaimSubmitConfirmationService(
            partyService,
            paymentService,
            objectMapper
        );
    }

    @Test
    void shouldReturnDefaultResponseWhenNoCounterClaimEntity() {
        PartyEntity partyEntity = PartyEntity.builder().build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder().build();
        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(
                possessionClaimResponse,
                null,
                null,
                false
            );

        SubmitResponse<State> response = underTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult, partyEntity);

        assertThat(response).isEqualTo(SubmitResponse.defaultResponse());
        verify(paymentService, never()).createServiceRequest(any());
    }

    @Test
    void shouldReturnConfirmationWithoutPaymentDetailsWhenPaymentNotRequired() {
        PartyEntity partyEntity = PartyEntity.builder().build();
        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.COUNTER_CLAIM_ISSUED)
            .build();
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder().build();
        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(
                possessionClaimResponse,
                counterClaimEntity,
                null,
                false
            );

        SubmitResponse<State> response = underTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult, partyEntity);

        assertThat(response.getConfirmationBody())
            .contains("\"status\":\"COUNTER_CLAIM_ISSUED\"")
            .contains("\"serviceRequestReference\":null")
            .contains("\"feeAmount\":null");
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

        FeeDetails feeDetails = FeeDetails.builder()
            .code("FEE_CODE")
            .description("Counterclaim fee")
            .feeAmount(FEE_AMOUNT)
            .version(1)
            .build();

        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            new RespondPossessionClaimSubmitPersistenceResult(
                possessionClaimResponse,
                counterClaimEntity,
                feeDetails,
                true
            );
        PartyEntity partyEntity = PartyEntity.builder().id(PARTY_ID).build();

        String currentUserPartyName = "Current user party name";

        when(partyService.getPartyName(partyEntity)).thenReturn(currentUserPartyName);
        when(paymentService.createServiceRequest(any(FeesAndPayTaskData.class)))
            .thenReturn(PaymentServiceResponse.builder()
                .serviceRequestReference(SERVICE_REQUEST_REFERENCE)
                .build());

        SubmitResponse<State> response = underTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult, partyEntity);

        verify(paymentService).createServiceRequest(taskDataCaptor.capture());
        FeesAndPayTaskData taskData = taskDataCaptor.getValue();
        assertThat(taskData.getFeeDetails()).isEqualTo(feeDetails);
        assertThat(taskData.getCcdCaseNumber()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getResponsiblePartyId()).isEqualTo(PARTY_ID);
        assertThat(taskData.getResponsiblePartyName()).isEqualTo(currentUserPartyName);
        assertThat(taskData.getPaymentCallbackHandlerType()).isEqualTo(COUNTER_CLAIM_ISSUE);
        assertThat(taskData.getRelatedEntityId()).isEqualTo(COUNTER_CLAIM_ID);

        assertThat(response.getConfirmationBody())
            .contains("\"status\":\"PENDING_COUNTER_CLAIM_ISSUED\"")
            .contains("\"serviceRequestReference\":\"" + SERVICE_REQUEST_REFERENCE + "\"")
            .contains("\"feeAmount\":80.00")
            .contains("\"claimType\":\"PAYMENT_OR_COMPENSATION\"");
    }

    @Test
    void shouldThrowWhenSuppliedPartyIsNullDuringPaymentCreation() {
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
                FeeDetails.builder().build(),
                true
            );

        assertThatThrownBy(() -> underTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Responsible party entity not provided");
    }

    @Test
    void shouldThrowWhenSubmitResponseCannotBeSerialised() throws Exception {
        PartyEntity partyEntity = PartyEntity.builder().build();
        ObjectMapper failingObjectMapper = spy(new ObjectMapper());
        CounterClaimSubmitConfirmationService serviceUnderTest = new CounterClaimSubmitConfirmationService(
            partyService,
            paymentService,
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
                null,
                false
            );

        doThrow(new JsonProcessingException("serialisation failed") {})
            .when(failingObjectMapper).writeValueAsString(any());

        assertThatThrownBy(() -> serviceUnderTest.buildSubmitResponse(CASE_REFERENCE, persistenceResult, partyEntity))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Unable to serialise respond possession claim submit response");
    }


}
