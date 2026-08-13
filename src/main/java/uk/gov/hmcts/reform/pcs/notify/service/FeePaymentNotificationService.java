package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeePaymentNotificationService {

    private final NotificationService notificationService;
    private final FeePaymentRepository feePaymentRepository;
    private final CamundaService camundaService;

    @Transactional
    public void sendClaimantPaidCaseIssuedNotification(Integer feePaymentId) {
        FeePaymentEntity feePayment = feePaymentRepository.findById(feePaymentId)
            .orElseThrow(() -> new FeePaymentNotFoundException(ErrorCode.FEE_PAYMENT_NOTIFICATION,
                                    RedactionContext.of("Fee payment not found: ", feePaymentId)));

        log.info("Sending claimant paid case issued notification for fee payment: {}", feePaymentId);

        ClaimEntity claimEntity = feePayment.getClaim();
        notificationService.sendClaimantClaimIssuedEmailNotification(claimEntity);

        if (claimEntity.getLanguageUsed() == LanguageUsed.ENGLISH) {
            PcsCaseEntity pcsCaseEntity = claimEntity.getPcsCase();
            camundaService.createTask(pcsCaseEntity.getCaseReference(), TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
        }
    }
}
