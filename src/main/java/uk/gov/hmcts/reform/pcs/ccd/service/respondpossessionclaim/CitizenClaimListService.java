package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CitizenClaimListService {

    private final PartyRepository partyRepository;

    public List<ClaimSummary> getClaimsAgainst(UUID idamId) {
        return partyRepository.findClaimsByDefendantIdamId(idamId, PartyRole.DEFENDANT).stream()
            .map(claim -> ClaimSummary.builder()
                .caseRef(String.valueOf(claim.getPcsCase().getCaseReference()))
                .claimantName(extractClaimantName(claim))
                .propertyPostcode(extractPostcode(claim))
                .build())
            .toList();
    }

    private String extractClaimantName(ClaimEntity claim) {
        return claim.getClaimParties().stream()
            .filter(cp -> cp.getRole() == PartyRole.CLAIMANT)
            .map(ClaimPartyEntity::getParty)
            .map(PartyEntity::getOrgName)
            .findFirst()
            .orElse(null);
    }

    private String extractPostcode(ClaimEntity claim) {
        return Optional.ofNullable(claim.getPcsCase().getPropertyAddress())
            .map(AddressEntity::getPostcode)
            .orElse(null);
    }
}
