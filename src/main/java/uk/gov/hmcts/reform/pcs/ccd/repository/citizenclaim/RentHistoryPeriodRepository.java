package uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.RentHistoryPeriodEntity;

import java.util.List;
import java.util.UUID;

public interface RentHistoryPeriodRepository extends JpaRepository<RentHistoryPeriodEntity, UUID> {

    List<RentHistoryPeriodEntity> findByClaimId(UUID claimId);

}
