package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneralApplicationRepository extends JpaRepository<GenApplication, UUID> {

    List<GenApplication> findByPcsCase_CcdCaseReference(long caseReference);

    void deleteByApplicationId(String applicationId);

    Optional<GenApplication> findByApplicationId(String caseRef);
}
