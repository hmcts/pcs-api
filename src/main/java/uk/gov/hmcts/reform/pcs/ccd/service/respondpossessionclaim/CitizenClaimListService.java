package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.ClaimantClaimSummary;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.ClaimantCaseSummaryRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CitizenClaimListService {

    private final PartyRepository partyRepository;
    private final ClaimantCaseSummaryRepository claimantCaseSummaryRepository;

    public List<ClaimSummary> getClaimsAgainst(String idamId) {
        return partyRepository.findClaimsByIdamIdAndRole(idamId, PartyRole.DEFENDANT).stream()
            .map(claim -> ClaimSummary.builder()
                .caseReference(String.valueOf(claim.getPcsCase().getCaseReference()))
                .claimantName(extractClaimantName(claim))
                .propertyPostcode(extractPostcode(claim))
                .build())
            .toList();
    }

    /**
     * The cases the citizen created as claimant, newest first, each with its case state and property address. All of
     * it comes from this service's own database in a single query, so no connection is held while calling CCD.
     */
    public List<ClaimantClaimSummary> getClaimsMadeBy(String idamId) {
        return claimantCaseSummaryRepository.findCasesCreatedBy(idamId);
    }

    private String extractClaimantName(ClaimEntity claim) {
        return claim.getClaimParties().stream()
            .filter(cp -> cp.getRole() == PartyRole.CLAIMANT)
            .map(ClaimPartyEntity::getParty)
            .map(claimant -> (claimant.getOrgName() != null
                ? claimant.getOrgName()
                : String.format("%s %s", claimant.getFirstName(), claimant.getLastName())))
            .findFirst()
            .orElse(null);
    }

    private String extractPostcode(ClaimEntity claim) {
        return Optional.ofNullable(claim.getPcsCase().getPropertyAddress())
            .map(AddressEntity::getPostcode)
            .orElse(null);
    }
}
