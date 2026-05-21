package uk.gov.hmcts.reform.pcs.testingsupport.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CounterClaimTestStatusService {

    private final CounterClaimRepository counterClaimRepository;

    @Transactional
    public void updateStatus(UUID counterClaimId, CounterClaimStatus newStatus) {
        CounterClaimEntity counterClaim = counterClaimRepository.findById(counterClaimId)
            .orElseThrow(() -> new IllegalArgumentException("Counterclaim not found: " + counterClaimId));

        counterClaim.setStatus(newStatus);
        counterClaimRepository.save(counterClaim);
    }
}
