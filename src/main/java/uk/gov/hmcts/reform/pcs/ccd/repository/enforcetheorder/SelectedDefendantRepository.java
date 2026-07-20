package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.SelectedDefendantEntity;


@Repository
public interface SelectedDefendantRepository extends JpaRepository<SelectedDefendantEntity, Long> {

}
