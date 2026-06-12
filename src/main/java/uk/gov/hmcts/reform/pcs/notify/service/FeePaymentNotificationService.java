package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeePaymentNotificationService {

    private final NotificationService notificationService;
    private final FeePaymentRepository feePaymentRepository;

    @Transactional
    public void sendClaimantPaidCaseIssuedNotification(UUID feePaymentId) {
        FeePaymentEntity feePayment = feePaymentRepository.findById(feePaymentId)
            .orElseThrow(() -> new FeePaymentNotFoundException("Fee payment not found: " + feePaymentId));

        log.info("Sending claimant paid case issued notification for fee payment: {}", feePaymentId);

        notificationService.sendClaimantClaimIssuedEmailNotification(feePayment.getClaim());
    }
}
