package uk.gov.hmcts.reform.pcs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.CourtEligibilityEntity;

public interface CourtEligibilityRepository extends JpaRepository<CourtEligibilityEntity, Integer> {

}
