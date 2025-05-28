package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePossessionGround;

import java.util.UUID;

public interface PossessionGroundRepository extends JpaRepository<CasePossessionGround, UUID> {

}
