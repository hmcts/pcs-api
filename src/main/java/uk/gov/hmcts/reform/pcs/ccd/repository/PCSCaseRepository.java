package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;

public interface PCSCaseRepository extends JpaRepository<PCS, Long> {

    PCS findByCcdCaseReference(Long caseReference);
}
