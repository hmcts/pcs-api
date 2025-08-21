package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimAction;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimState.AWAITING_SUBMISSION_TO_HMCTS;

@Service
@AllArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PcsCaseService pcsCaseService;
    private final CounterClaimEventService counterClaimEventService;
    private final ModelMapper modelMapper;

    public Claim getClaim(UUID claimId) {
        ClaimEntity claimEntity = getClaimEntity(claimId);

        return modelMapper.map(claimEntity, Claim.class);
    }

    public UUID createClaim(long caseReference,
                            ClaimType claimType,
                            String claimDetails) {

        PcsCaseEntity pcsCaseEntity = pcsCaseService.getCaseByCaseReference(caseReference);

        ClaimEntity claimEntity = ClaimEntity.builder()
            .type(claimType)
            .counterClaimState(AWAITING_SUBMISSION_TO_HMCTS)
            .created(Instant.now())
            .summary(claimDetails)
            .build();

        pcsCaseEntity.addClaim(claimEntity);

        claimRepository.save(claimEntity);

        return claimEntity.getId();
    }

    public UUID createClaim(long caseReference,
                            ClaimType claimType,
                            String claimDetails,
                            String applicantEmail,
                            String respondentEmail) {

        PcsCaseEntity pcsCaseEntity = pcsCaseService.getCaseByCaseReference(caseReference);

        ClaimEntity claimEntity = ClaimEntity.builder()
            .type(claimType)
            .counterClaimState(AWAITING_SUBMISSION_TO_HMCTS)
            .created(Instant.now())
            .summary(claimDetails)
            .applicantEmail(applicantEmail)
            .respondentEmail(respondentEmail)
            .build();

        pcsCaseEntity.addClaim(claimEntity);

        claimRepository.save(claimEntity);

        return claimEntity.getId();
    }

    public void setClaimState(UUID claimId, CounterClaimState counterClaimState) {
        ClaimEntity claimEntity = getClaimEntity(claimId);

        claimEntity.setCounterClaimState(counterClaimState);

        claimRepository.save(claimEntity);
    }

    public ClaimEntity createAndLinkClaim(PcsCaseEntity caseEntity, PartyEntity partyEntity,
                                          ClaimType claimType, PartyRole role) {

        ClaimEntity claim = ClaimEntity.builder()
            .type(claimType)
            .pcsCase(caseEntity)
            .build();

        caseEntity.getClaims().add(claim);
        claim.addParty(partyEntity, role);

        return claim;
    }


    public ClaimEntity saveClaim(ClaimEntity claim) {
        return claimRepository.save(claim);
    }

    public List<CounterClaimEvent> getApplicableCounterClaimEvents(UUID claimId) {
        CounterClaimState claimState = getClaimEntity(claimId).getCounterClaimState();
        if (claimState == null) {
            return List.of();
        }
        return getApplicableCounterClaimEvents(claimState);
    }

    public List<CounterClaimEvent> getApplicableCounterClaimEvents(CounterClaimState claimState) {
        if (claimState == null) {
            return List.of();
        }

        return counterClaimEventService.getAllEvents().stream()
            .filter(event -> event.isApplicableFor(claimState))
            .toList();
    }

    private ClaimEntity getClaimEntity(UUID claimId) {
        return claimRepository.findById(claimId)
            .orElseThrow(() -> new ClaimNotFoundException(claimId));
    }

}
