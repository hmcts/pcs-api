package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEventLogEntity;

import java.util.UUID;

public interface ClaimEventLogRespository extends JpaRepository<ClaimEventLogEntity, UUID> {

}
