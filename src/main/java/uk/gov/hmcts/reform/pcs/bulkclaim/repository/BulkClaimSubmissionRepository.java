package uk.gov.hmcts.reform.pcs.bulkclaim.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BulkClaimSubmissionRepository extends JpaRepository<BulkClaimSubmissionEntity, UUID> {

    Optional<BulkClaimSubmissionEntity> findByOrganisationIdAndCustomerUniqueIdentifier(
        String organisationId, String customerUniqueIdentifier);

    List<BulkClaimSubmissionEntity> findByOrganisationIdAndCustomerUniqueIdentifierIn(
        String organisationId, Collection<String> customerUniqueIdentifiers);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BulkClaimSubmissionEntity s WHERE s.id = :id")
    Optional<BulkClaimSubmissionEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<BulkClaimSubmissionEntity> findByServiceRequestReference(String serviceRequestReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BulkClaimSubmissionEntity s WHERE s.serviceRequestReference = :reference")
    Optional<BulkClaimSubmissionEntity> findByServiceRequestReferenceForUpdate(@Param("reference") String reference);

    Optional<BulkClaimSubmissionEntity> findByCaseReference(Long caseReference);

}
