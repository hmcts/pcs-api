package uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;

import java.util.Optional;
import java.util.UUID;

public interface FeePaymentRepository extends JpaRepository<FeePaymentEntity, Integer> {

    Optional<FeePaymentEntity> findByServiceRequestReference(String serviceRequestReference);

    Optional<FeePaymentEntity> findByRelatedEntityId(UUID relatedEntityId);

    /**
     * Adds a write lock on the fee payment row so a duplicate delivery of the same payment webhook
     * waits for the in-flight transaction to commit, rather than reading the same pre-update state.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "15000"))
    @Query("select f from FeePaymentEntity f where f.serviceRequestReference = :serviceRequestReference")
    Optional<FeePaymentEntity> findByServiceRequestReferenceForUpdate(
        @Param("serviceRequestReference") String serviceRequestReference);

}
