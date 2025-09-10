package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimGroundRepository;

@Service
@AllArgsConstructor
public class ClaimGroundService {

    private final ClaimGroundRepository claimGroundRepository;

    public ClaimGroundEntity saveClaimGround(ClaimGroundEntity groundEntity) {
        return claimGroundRepository.save(groundEntity);
    }
}
