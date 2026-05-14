package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdUpdateService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.JourneyId;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeAClaimPaymentCallbackStrategyTest {

    private static final String CCD_CASE_NUMBER = "1111-2222-3333-4444";
    private static final String RESPONSIBLE_PARTY = "Claimant Org Ltd";

    @Mock
    private CcdUpdateService ccdUpdateService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MakeAClaimPaymentCallbackStrategy underTest;

    @Test
    void shouldSetPartyOnEntityAndSubmitPaymentSuccess() throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData(RESPONSIBLE_PARTY);
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        String taskDataJson = new ObjectMapper().writeValueAsString(taskData);
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).orgName(RESPONSIBLE_PARTY).build();
        ClaimEntity claimEntity = new ClaimEntity();
        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder().claim(claimEntity).party(partyEntity).build();
        claimEntity.setClaimParties(List.of(claimPartyEntity));

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(claimEntity).taskData(taskDataJson)
            .journeyId(JourneyId.RESUME_POSSESSION_CLAIM).build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When
        underTest.handle(callback, feePaymentEntity);

        // Then
        assertThat(feePaymentEntity.getParty()).isSameAs(partyEntity);
        verify(ccdUpdateService).submitPaymentSuccess(taskData.getCaseReference());
    }

    @Test
    void shouldThrowPaymentCallbackExceptionWhenTaskDataIsInvalidJson() throws Exception {
        // Given
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class)))
            .thenThrow(new JsonProcessingException("Invalid JSON") {});
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(new ClaimEntity()).taskData("aasdfsdf{{")
            .journeyId(JourneyId.RESUME_POSSESSION_CLAIM).build();

        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When / Then
        assertThatExceptionOfType(PaymentCallbackException.class)
            .isThrownBy(() -> underTest.handle(callback, feePaymentEntity))
            .withMessageContaining("Unable to process");
    }

    @Test
    void shouldThrowPartyNotFoundExceptionWhenNoClaimPartyMatchesResponsibleParty() throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData("XYZ Org");
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        String taskDataJson = new ObjectMapper().writeValueAsString(taskData);

        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).orgName(RESPONSIBLE_PARTY).build();
        ClaimEntity claimEntity = new ClaimEntity();
        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder().claim(claimEntity).party(partyEntity).build();
        claimEntity.setClaimParties(List.of(claimPartyEntity));
        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder().claim(claimEntity).taskData(taskDataJson)
            .journeyId(JourneyId.RESUME_POSSESSION_CLAIM).build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When / Then
        assertThatExceptionOfType(PartyNotFoundException.class)
            .isThrownBy(() -> underTest.handle(callback, feePaymentEntity));
    }

    @Test
    void shouldNotCallCcdUpdateServiceWhenPartyIsNotFound() throws Exception {
        // Given
        FeesAndPayTaskData taskData = buildTaskData("Unknown Org");
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);

        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).orgName(RESPONSIBLE_PARTY).build();
        ClaimEntity claimEntity = new ClaimEntity();
        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder().claim(claimEntity).party(partyEntity).build();
        claimEntity.setClaimParties(List.of(claimPartyEntity));

        FeePaymentEntity feePaymentEntity = FeePaymentEntity.builder()
            .claim(claimEntity)
            .taskData("{}")
            .journeyId(JourneyId.RESUME_POSSESSION_CLAIM)
            .build();
        PaymentStatusCallback callback = PaymentStatusCallback.builder().ccdCaseNumber(CCD_CASE_NUMBER).build();

        // When / Then
        assertThatExceptionOfType(PartyNotFoundException.class)
            .isThrownBy(() -> underTest.handle(callback, feePaymentEntity));

        verifyNoInteractions(ccdUpdateService);
    }

    private FeesAndPayTaskData buildTaskData(String responsibleParty) {
        return FeesAndPayTaskData.builder()
            .caseReference("1234")
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .responsibleParty(responsibleParty)
            .journeyId(JourneyId.RESUME_POSSESSION_CLAIM)
            .feeDetails(FeeDetails.builder().feeAmount(new BigDecimal("232.00")).code("FEE0412").build())
            .build();
    }
}
