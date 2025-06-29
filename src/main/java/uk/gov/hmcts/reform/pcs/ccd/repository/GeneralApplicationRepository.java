package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.GA;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneralApplicationRepository extends JpaRepository<GA, UUID> {

    List<GA> findByPcsCase_CaseReference(long caseReference);

    void deleteByCaseReference(Long caseReference);

    Optional<GA> findByCaseReference(Long caseRef);
}
