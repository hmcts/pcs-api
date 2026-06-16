package uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;

import java.util.Optional;
import java.util.UUID;

public interface FeePaymentRepository extends JpaRepository<FeePaymentEntity, UUID> {

    Optional<FeePaymentEntity> findByServiceRequestReference(String serviceRequestReference);

    Optional<FeePaymentEntity> findByRelatedEntityId(UUID relatedEntityId);

    Optional<FeePaymentEntity> findByRelatedEntityIdAndPaymentCallbackHandlerType(
        UUID relatedEntityId,
        PaymentCallbackHandlerType paymentCallbackHandlerType
    );

}
