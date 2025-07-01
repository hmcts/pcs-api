package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.GA;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneralApplicationRepository extends JpaRepository<GA, UUID> {

    List<GA> findByPcsCase_CaseReference(Long caseReference);

    Optional<GA> findByCaseReference(Long caseRef);

    Optional<GA> findById(UUID id);
}
