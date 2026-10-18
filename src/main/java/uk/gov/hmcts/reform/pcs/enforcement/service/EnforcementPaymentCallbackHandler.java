package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementOrderNotFoundException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentCallbackException;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentCallbackStrategy;

import java.util.UUID;

/**
 * Handles the Payments callback for an enforcement application's fee. The fee payment's related entity is the
 * enforcement (enf_case) the fee is for. Paying moves the enforcement from awaiting payment to awaiting review;
 * the CCD case state is not changed.
 *
 * <p>The fee payment's party is deliberately left unset, as for a general application: a claimant party on a
 * paid fee payment makes {@code FeePaymentEntityListener} send the claim issued email.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnforcementPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final EnforcementOrderService enforcementOrderService;

    @Override
    public PaymentCallbackHandlerType handlerType() {
        return PaymentCallbackHandlerType.ENFORCEMENT;
    }

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        UUID enforcementId = feePaymentEntity.getRelatedEntityId();
        log.info("Handling callback for enforcement payment for enforcement {}", enforcementId);

        if (feePaymentEntity.getPaymentStatus() != PaymentStatus.PAID) {
            log.warn("The payment was not successful [{}] for enforcement {} on case {}",
                     feePaymentEntity.getPaymentStatus(), enforcementId, paymentStatusCallback.getCcdCaseNumber());
            return;
        }
        if (enforcementId == null) {
            throw new PaymentCallbackException("Enforcement payment callback has no related enforcement", null);
        }

        EnforcementOrderEntity enforcement = enforcementOrderRepository.findById(enforcementId)
            .orElseThrow(() -> new EnforcementOrderNotFoundException(enforcementId));
        if (enforcement.getStatus() == EnforcementStatus.AWAITING_PAYMENT) {
            long caseReference = enforcement.getClaim().getPcsCase().getCaseReference();
            enforcementOrderService.moveToAwaitingReview(caseReference, enforcement);
        } else {
            log.warn("Enforcement {} status {} not valid for this callback", enforcementId, enforcement.getStatus());
        }
    }

}
