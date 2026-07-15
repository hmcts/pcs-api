package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class CounterClaimService {

    private final PartyRepository partyRepository;
    private final ClaimRepository claimRepository;
    private final CounterClaimRepository counterClaimRepository;
    private final Clock utcClock;

    public CounterClaimService(PartyRepository partyRepository,
                               ClaimRepository claimRepository,
                               CounterClaimRepository counterClaimRepository,
                               @Qualifier("utcClock") Clock utcClock) {
        this.partyRepository = partyRepository;
        this.claimRepository = claimRepository;
        this.counterClaimRepository = counterClaimRepository;
        this.utcClock = utcClock;
    }

    public Optional<CounterClaimEntity> saveCounterClaim(
        long caseReference,
        CounterClaim counterClaim,
        PartyEntity defendantParty
    ) {
        if (defendantParty == null) {
            throw new IllegalStateException("Defendant party is null for case: " + caseReference);
        }

        if (counterClaim == null) {
            return Optional.empty();
        }

        UUID claimId = claimRepository.findIdByCaseReference(caseReference)
            .orElseThrow(() -> new IllegalStateException("No claim found for case: " + caseReference));
        ClaimEntity claimRef = claimRepository.getReferenceById(claimId);

        CounterClaimEntity counterClaimEntity = buildCounterClaimEntity(
            counterClaim, defendantParty, LocalDateTime.now(utcClock));
        counterClaimEntity.setPcsCase(claimRef.getPcsCase());
        CounterClaimEntity savedCounterClaim = counterClaimRepository.save(counterClaimEntity);
        log.info("Saved counterclaim {} for case {}", savedCounterClaim.getId(), caseReference);
        return Optional.of(savedCounterClaim);
    }

    public CounterClaimEntity buildCounterClaimEntity(CounterClaim counterClaim,
                                                      PartyEntity partyRef,
                                                      LocalDateTime submittedAt) {
        boolean claimAmountApplies = counterClaim.getClaimType() != null
            && counterClaim.getClaimType() != CounterClaimType.SOMETHING_ELSE;

        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .claimType(counterClaim.getClaimType())
            .isClaimAmountKnown(claimAmountApplies ? counterClaim.getIsClaimAmountKnown() : null)
            .claimAmount(claimAmountApplies && counterClaim.getIsClaimAmountKnown() == VerticalYesNo.YES
                ? counterClaim.getClaimAmount() : null)
            .estimatedMaxClaimAmount(claimAmountApplies && counterClaim.getIsClaimAmountKnown() == VerticalYesNo.NO
                ? counterClaim.getEstimatedMaxClaimAmount() : null)
            .counterClaimFor(counterClaim.getCounterClaimFor())
            .counterClaimReasons(counterClaim.getCounterClaimReasons())
            .otherOrderRequestDetails(counterClaim.getClaimType() == CounterClaimType.SOMETHING_ELSE
                ? counterClaim.getOtherOrderRequestDetails() : null)
            .otherOrderRequestFacts(counterClaim.getClaimType() == CounterClaimType.SOMETHING_ELSE
                ? counterClaim.getOtherOrderRequestFacts() : null)
            .needHelpWithFees(counterClaim.getNeedHelpWithFees())
            .appliedForHwf(counterClaim.getAppliedForHwf())
            .hwfReferenceNumber(counterClaim.getHwfReferenceNumber())
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .claimSubmittedDate(submittedAt)
            .party(partyRef)
            .build();

        if (counterClaim.getCounterClaimAgainst() != null) {
            counterClaimEntity.getCounterClaimParties().addAll(
                counterClaim.getCounterClaimAgainst().stream()
                    .filter(lv -> lv.getId() != null)
                    .map(lv -> CounterClaimPartyEntity.builder()
                        .counterClaim(counterClaimEntity)
                        .party(partyRepository.getReferenceById(UUID.fromString(lv.getId())))
                        .build())
                    .toList()
            );
        }

        return counterClaimEntity;
    }

    public CounterClaimEntity issueCounterClaim(CounterClaimEntity counterClaimEntity) {
        counterClaimEntity.setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED);
        counterClaimEntity.setClaimIssuedDate(LocalDateTime.now(utcClock));
        CounterClaimEntity issuedCounterClaim = counterClaimRepository.save(counterClaimEntity);
        log.info("Issued counterclaim {}", issuedCounterClaim.getId());
        return issuedCounterClaim;
    }
}
