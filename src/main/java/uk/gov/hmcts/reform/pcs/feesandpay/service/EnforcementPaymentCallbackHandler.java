package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementStateEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementStateService;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.CASE_PROGRESSION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus.PAID;

@Component
@Slf4j
@RequiredArgsConstructor
public class EnforcementPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final EnforcementStateService enforcementStateService;
    private final EnforcementOrderRepository enforcementOrderRepository;

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        UUID enfApplicationId = feePaymentEntity.getRelatedEntityId();

        log.info("Handling callback for enforcement app payment for case number ID {}",
                paymentStatusCallback.getCcdCaseNumber());

        if (feePaymentEntity.getPaymentStatus() == PAID) {
            EnforcementOrderEntity enforcementOrderEntity = findEnfApplicationEntity(enfApplicationId);
            EnforcementStateEntity enforcementStateEntity = enforcementOrderEntity.getPcsCase().getEnforcementState();
            EnforcementState enforcementState = enforcementStateEntity.getState();
            if (enforcementState == PENDING_CASE_ISSUED) {
                enforcementStateService.mapToEntity(enforcementOrderEntity.getPcsCase(), CASE_PROGRESSION);
                enforcementOrderRepository.save(enforcementOrderEntity);
                log.info("Enforcement app {} state updated to CASE_PROGRESSION", enfApplicationId);
            } else {
                log.warn("Enforcement app {} state {} not valid for this callback", enfApplicationId,
                        enforcementState);
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
