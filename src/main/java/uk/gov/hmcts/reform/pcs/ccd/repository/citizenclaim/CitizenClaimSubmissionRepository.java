package uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.CitizenClaimSubmissionEntity;

import java.util.List;
import java.util.UUID;

public interface CitizenClaimSubmissionRepository extends JpaRepository<CitizenClaimSubmissionEntity, UUID> {

    List<CitizenClaimSubmissionEntity> findByClaimId(UUID claimId);

}
