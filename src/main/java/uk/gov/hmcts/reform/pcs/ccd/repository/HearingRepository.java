package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;

public interface HearingRepository extends JpaRepository<HearingEntity, Integer> {

}
