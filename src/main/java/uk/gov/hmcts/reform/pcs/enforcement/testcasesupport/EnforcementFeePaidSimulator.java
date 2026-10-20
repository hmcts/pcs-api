package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementPaymentCallbackHandler;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class EnforcementFeePaidSimulator {

    private final PcsCaseService pcsCaseService;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final PaymentService paymentService;
    private final EnforcementPaymentCallbackHandler enforcementPaymentCallbackHandler;

    @Transactional
    public boolean recordFeePaid(long caseReference) {
        ClaimEntity claim = Optional.ofNullable(pcsCaseService.loadCase(caseReference).getMainClaim())
            .orElseThrow(() -> new ClaimNotFoundException(caseReference));
        Optional<EnforcementOrderEntity> awaitingPayment = enforcementOrderRepository
            .findByClaimIdAndStatus(claim.getId(), EnforcementStatus.AWAITING_PAYMENT)
            .stream()
            .findFirst();
        awaitingPayment.ifPresent(enforcement -> pay(caseReference, enforcement));
        return awaitingPayment.isPresent();
    }

    private void pay(long caseReference, EnforcementOrderEntity enforcement) {
        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .ccdCaseNumber(String.valueOf(caseReference))
            .serviceRequestStatus(PaymentStatus.PAID.getValue())
            .build();

        feePaymentRepository.findByRelatedEntityId(enforcement.getId())
            .filter(feePayment -> feePayment.getPaymentCallbackHandlerType() == PaymentCallbackHandlerType.ENFORCEMENT
                && feePayment.getServiceRequestReference() != null)
            .ifPresentOrElse(
                feePayment -> {
                    log.info("Sending a paid callback for service request {} on case {}",
                             feePayment.getServiceRequestReference(), caseReference);
                    callback.setServiceRequestReference(feePayment.getServiceRequestReference());
                    paymentService.processPaymentResponse(callback);
                },
                () -> {
                    log.info("No fee payment for enforcement {} on case {}, so paying it without a service request",
                             enforcement.getId(), caseReference);
                    enforcementPaymentCallbackHandler.handle(callback, FeePaymentEntity.builder()
                        .claim(enforcement.getClaim())
                        .paymentCallbackHandlerType(PaymentCallbackHandlerType.ENFORCEMENT)
                        .paymentStatus(PaymentStatus.PAID)
                        .relatedEntityId(enforcement.getId())
                        .build());
                });
    }
}
