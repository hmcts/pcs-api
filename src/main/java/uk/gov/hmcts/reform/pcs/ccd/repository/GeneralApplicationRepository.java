package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;

import java.util.List;

public interface GeneralApplicationRepository extends JpaRepository<GenApplication, Long> {

    List<GenApplication> findByPcsCase_CcdCaseReference(Long caseReference);
}
