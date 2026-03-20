package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;

import java.util.UUID;

public interface StatementOfTruthRepository extends JpaRepository<StatementOfTruthEntity, UUID>  {
}
