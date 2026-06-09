package uk.gov.hmcts.reform.pcs.testingsupport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntityTestStatusService {

    private final CounterClaimRepository counterClaimRepository;
    private final DefendantResponseRepository defendantResponseRepository;

    @Transactional
    public void updateCounterClaimState(UUID counterClaimId, CounterClaimState newStatus) {
        CounterClaimEntity counterClaim = counterClaimRepository.findById(counterClaimId)
            .orElseThrow(() -> new IllegalArgumentException("Counterclaim not found: " + counterClaimId));

        counterClaim.setStatus(newStatus);
        counterClaimRepository.save(counterClaim);
    }

    @Transactional
    public void updateDefendantResponseStatus(UUID defendantResponseId, DefendantResponseStatus newStatus) {
        DefendantResponseEntity defendantResponse = defendantResponseRepository.findById(defendantResponseId)
            .orElseThrow(() -> new IllegalArgumentException("Defendant response not found: " + defendantResponseId));

        defendantResponse.setStatus(newStatus);
        defendantResponseRepository.save(defendantResponse);
    }
}
