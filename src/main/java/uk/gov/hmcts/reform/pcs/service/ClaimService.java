package uk.gov.hmcts.reform.pcs.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.entity.Claim;
import uk.gov.hmcts.reform.pcs.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PartyRepository partyRepository;

    public ClaimService(ClaimRepository claimRepository,
                        PartyRepository partyRepository) {
        this.claimRepository = claimRepository;
        this.partyRepository = partyRepository;
    }

    public void linkClaimants(UUID claimId, List<UUID> partyIds) {
        linkParties(claimId, partyIds, PartyRole.CLAIMANT);
    }

    public void linkDefendants(UUID claimId, List<UUID> partyIds) {
        linkParties(claimId, partyIds, PartyRole.DEFENDANT);
    }

    public void linkInterestedParties(UUID claimId, List<UUID> partyIds) {
        linkParties(claimId, partyIds, PartyRole.INTERESTED_PARTY);
    }

    private void linkParties(UUID claimId, List<UUID> partyIds, PartyRole partyRole) {
        Claim claim = claimRepository.getReferenceById(claimId);

        partyRepository.findAllById(partyIds)
            .forEach(partyEntity -> claim.addParty(partyEntity, partyRole));

        claimRepository.save(claim);
    }

}
