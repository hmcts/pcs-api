package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.warrant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant.SelectedDefendantEntity;

import java.util.UUID;

@Repository
public interface SelectedDefendantRepository extends
    JpaRepository<SelectedDefendantEntity, UUID> {
}
