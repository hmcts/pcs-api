package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.Optional;
import java.util.UUID;

public interface PcsCaseRepository extends JpaRepository<PcsCaseEntity, UUID> {

    // tenancyLicence is the non-owning side of a @OneToOne, so Hibernate loads it regardless;
    // fetching it here folds that into the main query instead of a separate select.
    @EntityGraph(attributePaths = {"propertyAddress", "parties", "parties.address", "tenancyLicence"})
    Optional<PcsCaseEntity> findByCaseReference(long caseReference);

}
