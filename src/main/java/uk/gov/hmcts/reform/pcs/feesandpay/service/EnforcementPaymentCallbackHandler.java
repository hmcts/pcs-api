package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class EnforcementPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final EnforcementOrderRepository enforcementOrderRepository;

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        UUID enfApplicationId = feePaymentEntity.getRelatedEntityId();

        log.info("Handling callback for enforcement app payment for case number ID {}",
                paymentStatusCallback.getCcdCaseNumber());

        if (feePaymentEntity.getPaymentStatus() == PaymentStatus.PAID) {
            EnforcementOrderEntity enforcementOrderEntity = findEnfApplicationEntity(enfApplicationId);
            if (enforcementOrderEntity.getState() == EnforcementState.PENDING_CASE_ISSUED) {
                enforcementOrderEntity.setState(EnforcementState.CASE_PROGRESSION);
                enforcementOrderRepository.save(enforcementOrderEntity);
            } else {
                log.warn("Enforcement app {} state {} not valid for this callback", enfApplicationId,
                        enforcementOrderEntity.getState());
            }
        } else {
            log.warn("The payment was not successful [{}] for enforcement app {} on case {}",
                     feePaymentEntity.getPaymentStatus(), enfApplicationId, paymentStatusCallback.getCcdCaseNumber());
        }
    }

    private EnforcementOrderEntity findEnfApplicationEntity(UUID enfAppId) {
        return enforcementOrderRepository.findById(enfAppId)
                .orElseThrow(() -> new EnforcementApplicationNotFoundException("Unable to find Enforcement app with ID "
                        + enfAppId));
    }
}
