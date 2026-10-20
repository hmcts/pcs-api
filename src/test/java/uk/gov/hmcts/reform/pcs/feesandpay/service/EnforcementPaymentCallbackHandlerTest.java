package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforcementPayment;

@ExtendWith(MockitoExtension.class)
class EnforcementPaymentCallbackHandlerTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID RESPONSIBLE_PARTY_ID = UUID.randomUUID();

    @Mock
    private CcdPaymentStateUpdateService ccdPaymentStateUpdateService;
    @Mock
    private PartyService partyService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EnforcementPaymentCallbackHandler underTest;

    @ParameterizedTest
    @MethodSource("paymentStatuses")
    void shouldSetResponsiblePartyAndSubmitSuccessOnlyWhenPaid(PaymentStatus paymentStatus) throws Exception {
        // Given
        FeesAndPayTaskData taskData = FeesAndPayTaskData.builder()
            .caseReference(CASE_REFERENCE)
            .responsiblePartyId(RESPONSIBLE_PARTY_ID)
            .build();
        when(objectMapper.readValue(anyString(), eq(FeesAndPayTaskData.class))).thenReturn(taskData);
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        when(partyService.getPartyEntityByEntityId(RESPONSIBLE_PARTY_ID, CASE_REFERENCE)).thenReturn(party);
        FeePaymentEntity feePayment = FeePaymentEntity.builder().taskData("task data").paymentStatus(paymentStatus)
            .build();

        // When
        underTest.handle(PaymentStatusCallback.builder().build(), feePayment);

        // Then
        assertThat(feePayment.getParty()).isSameAs(party);
        if (paymentStatus == PaymentStatus.PAID) {
            verify(ccdPaymentStateUpdateService).submitPaymentSuccess(CASE_REFERENCE, enforcementPayment);
        } else {
            verifyNoInteractions(ccdPaymentStateUpdateService);
        }
    }

    private static Stream<PaymentStatus> paymentStatuses() {
        return Stream.of(PaymentStatus.values());
    }

}
