package uk.gov.hmcts.reform.pcs.ccd3.service;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd3.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd3.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd3.repository.ClaimRepository;

@Service
@AllArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;

    public ClaimEntity createAndLinkClaim(PcsCaseEntity caseEntity, PartyEntity partyEntity,
                                          String claimName, PartyRole role) {
        ClaimEntity claim = ClaimEntity.builder()
            .summary(claimName)
            .pcsCase(caseEntity)
            .build();

        caseEntity.getClaims().add(claim);
        claim.addParty(partyEntity, role);

        return claim;
    }


    public ClaimEntity saveClaim(ClaimEntity claim) {
        return claimRepository.save(claim);
    }
}
