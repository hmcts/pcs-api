package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeePaymentNotificationServiceTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private CamundaService camundaService;

    @InjectMocks
    private FeePaymentNotificationService underTest;

    @Test
    void shouldSendClaimantClaimIssuedEmailNotification() {
        UUID feePaymentId = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(1234L).build();
        ClaimEntity claim = ClaimEntity.builder().pcsCase(pcsCaseEntity).build();
        FeePaymentEntity feePayment = FeePaymentEntity.builder()
            .id(feePaymentId)
            .claim(claim)
            .build();
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));

        underTest.sendClaimantPaidCaseIssuedNotification(feePaymentId);

        verify(notificationService).sendClaimantClaimIssuedEmailNotification(claim);
        verify(camundaService).createTask(1234L, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
    }

    @Test
    void shouldThrowExceptionWhenFeePaymentNotFound() {
        UUID feePaymentId = UUID.randomUUID();
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.sendClaimantPaidCaseIssuedNotification(feePaymentId))
            .isInstanceOf(FeePaymentNotFoundException.class)
            .hasMessageContaining("Fee payment not found: " + feePaymentId);
    }
}
