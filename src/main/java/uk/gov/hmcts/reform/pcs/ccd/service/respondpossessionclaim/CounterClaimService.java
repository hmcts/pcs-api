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
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class CounterClaimService {

    private final PartyService partyService;
    private final PartyRepository partyRepository;
    private final ClaimRepository claimRepository;
    private final CounterClaimRepository counterClaimRepository;
    private final SecurityContextService securityContextService;
    private final Clock utcClock;

    public CounterClaimService(PartyService partyService,
                               PartyRepository partyRepository,
                               ClaimRepository claimRepository,
                               CounterClaimRepository counterClaimRepository,
                               SecurityContextService securityContextService,
                               @Qualifier("utcClock") Clock utcClock) {
        this.partyService = partyService;
        this.partyRepository = partyRepository;
        this.claimRepository = claimRepository;
        this.counterClaimRepository = counterClaimRepository;
        this.securityContextService = securityContextService;
        this.utcClock = utcClock;
    }

    public Optional<CounterClaimEntity> saveCounterClaim(long caseReference, CounterClaim counterClaim) {
        if (counterClaim == null) {
            return Optional.empty();
        }

        UUID userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Current user IDAM ID is null");
        }

        PartyEntity partyRef = partyRepository.getReferenceById(
            partyService.getPartyEntityByIdamId(userId, caseReference).getId()
        );

        UUID claimId = claimRepository.findIdByCaseReference(caseReference)
            .orElseThrow(() -> new IllegalStateException("No claim found for case: " + caseReference));
        ClaimEntity claimRef = claimRepository.getReferenceById(claimId);

        CounterClaimEntity counterClaimEntity = buildCounterClaimEntity(counterClaim, partyRef, claimRef);
        CounterClaimEntity savedCounterClaim = counterClaimRepository.save(counterClaimEntity);
        log.info("Saved counterclaim {} for case {}", savedCounterClaim.getId(), caseReference);
        return Optional.of(savedCounterClaim);
    }

    private CounterClaimEntity buildCounterClaimEntity(CounterClaim counterClaim,
                                                       PartyEntity partyRef,
                                                       ClaimEntity claimRef) {
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
            .claimSubmittedDate(LocalDateTime.now(utcClock))
            .party(partyRef)
            .pcsCase(claimRef.getPcsCase())
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
