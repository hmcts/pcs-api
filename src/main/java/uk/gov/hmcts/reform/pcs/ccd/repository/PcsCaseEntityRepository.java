package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

public interface PcsCaseEntityRepository extends JpaRepository<PcsCaseEntity, Long> {

}
