package uk.gov.hmcts.reform.pcs.ccd.service;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;

@Service
@AllArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;

    public ClaimEntity createAndLinkClaim(PcsCaseEntity caseEntity, PartyEntity partyEntity,
                                          String claimName, PartyRole role, Boolean costsClaimed) {
        ClaimEntity claim = ClaimEntity.builder()
            .summary(claimName)
            .pcsCase(caseEntity)
            .costsClaimed(costsClaimed)
            .build();

        caseEntity.getClaims().add(claim);
        claim.addParty(partyEntity, role);

        return claim;
    }

    public ClaimEntity saveClaim(ClaimEntity claim) {
        return claimRepository.save(claim);
    }
}
