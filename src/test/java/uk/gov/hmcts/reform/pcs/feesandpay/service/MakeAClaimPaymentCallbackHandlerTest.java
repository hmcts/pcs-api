package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackScheduler;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeAClaimPaymentCallbackHandlerTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String CCD_CASE_NUMBER = "1111-2222-3333-4444";
    private static final String RESPONSIBLE_PARTY = "Claimant Org Ltd";
    private static final UUID RESPONSIBLE_PARTY_ID = UUID.randomUUID();

    @Mock
    private CcdPaymentStateUpdateService ccdPaymentStateUpdateService;
    @Mock
    private PartyService partyService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ClaimPackScheduler claimPackScheduler;

    @InjectMocks
    private MakeAClaimPaymentCallbackHandler underTest;

    @ParameterizedTest
    @MethodSource("paymentStatus")
    void shouldSetPartyOnEntityAndSubmitPaymentSuccess(PaymentStatus paymentStatus) throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        String taskDataJson = new ObjectMapper().writeValueAsString(taskData);

        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).orgName(RESPONSIBLE_PARTY).build();
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE)).thenReturn(partyEntity);

        ClaimEntity claimEntity = new ClaimEntity();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(taskData.getCaseReference()).build();
        claimEntity.setPcsCase(pcsCase);
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(claimEntity).taskData(taskDataJson)
            .paymentStatus(paymentStatus).paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When
        underTest.handle(callback, feePaymentEntity);

        // Then
        assertThat(feePaymentEntity.getParty()).isSameAs(partyEntity);
        if (PaymentStatus.PAID == feePaymentEntity.getPaymentStatus()) {
            verify(ccdPaymentStateUpdateService).submitPaymentSuccess(taskData.getCaseReference());
            // §3.1 producer-side gate: scheduler is invoked ONLY on PAID.
            verify(claimPackScheduler).scheduleClaimPackGeneration(taskData.getCaseReference());
        } else {
            verifyNoInteractions(ccdPaymentStateUpdateService);
            verifyNoInteractions(claimPackScheduler);
        }
    }

    @Test
    void shouldThrowPaymentCallbackExceptionWhenTaskDataIsInvalidJson() throws Exception {
        // Given
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class)))
            .thenThrow(new JsonProcessingException("Invalid JSON") {});
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(new ClaimEntity()).taskData("aasdfsdf{{")
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();

        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When / Then
        assertThatExceptionOfType(PaymentCallbackException.class)
            .isThrownBy(() -> underTest.handle(callback, feePaymentEntity))
            .withMessageContaining("Unable to process");
        verifyNoInteractions(ccdPaymentStateUpdateService);
        verifyNoInteractions(claimPackScheduler);
    }

    @Test
    void shouldPropagatePartyNotFoundExceptionWhenNoPartyMatchesResponsibleParty() throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        String taskDataJson = "some task json";

        PartyNotFoundException expectedException = mock(PartyNotFoundException.class);
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE))
            .thenThrow(expectedException);

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().taskData(taskDataJson)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM).build();

        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When
        Throwable throwable = catchThrowable(() -> underTest.handle(callback, feePaymentEntity));

        // Then
        assertThat(throwable).isEqualTo(expectedException);
        verifyNoInteractions(ccdPaymentStateUpdateService);
        verifyNoInteractions(claimPackScheduler);
    }

    private FeesAndPayTaskData buildTaskData() {
        return FeesAndPayTaskData.builder()
            .caseReference(CASE_REFERENCE)
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .responsiblePartyId(RESPONSIBLE_PARTY_ID)
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM)
            .feeDetails(FeeDetails.builder().feeAmount(new BigDecimal("232.00")).code("FEE0412").build())
            .build();
    }

    private static Stream<PaymentStatus> paymentStatus() {
        return Stream.of(PaymentStatus.values());
    }

}
