package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.enforcement.entity.SelectedDefendantEntity;


@Repository
public interface SelectedDefendantRepository extends JpaRepository<SelectedDefendantEntity, Integer> {

}
