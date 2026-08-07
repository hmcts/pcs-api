package uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;

import java.util.Optional;
import java.util.UUID;

public interface FeePaymentRepository extends JpaRepository<FeePaymentEntity, Integer> {

    Optional<FeePaymentEntity> findByServiceRequestReference(String serviceRequestReference);

    Optional<FeePaymentEntity> findByRelatedEntityId(UUID relatedEntityId);

    @Query("SELECT f.claim.pcsCase.caseReference FROM FeePaymentEntity f "
        + "WHERE f.serviceRequestReference = :reference")
    Optional<Long> findCaseReferenceByServiceRequestReference(@Param("reference") String reference);

}
