package uk.gov.hmcts.reform.pcs.ccd.service.caseworker.manageparty;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.RemovePartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RemovePartyService {

    public static final String LAST_PARTY_ERROR =
        "You cannot remove a claimant or defendant if only one of these parties exist on the case.";
    public static final String OPEN_APPLICATION_OR_COUNTERCLAIM_ERROR =
        "You cannot remove this party while they have an open general application or counterclaim";
    public static final String PARTY_CANNOT_BE_REMOVED_ERROR = "Party cannot be removed";

    private static final List<GenAppState> OPEN_GEN_APP_STATES = List.of(
        GenAppState.PENDING_GEN_APP_ISSUED,
        GenAppState.GEN_APP_ISSUED
    );
    private static final List<CounterClaimState> OPEN_COUNTER_CLAIM_STATES = List.of(
        CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED,
        CounterClaimState.COUNTER_CLAIM_ISSUED
    );

    private final PartyService partyService;
    private final PartyRepository partyRepository;
    private final GenAppRepository genAppRepository;
    private final CounterClaimRepository counterClaimRepository;

    @Transactional
    public RemovedParty removeParty(RemovePartyDetails removePartyDetails, long caseReference) {
        if (removePartyDetails.getRemoveSelectedParty() == YesOrNo.NO) {
            throw new IllegalStateException(PARTY_CANNOT_BE_REMOVED_ERROR);
        }

        UUID partyId = removePartyDetails.getPartyToRemove().getValueCode();
        PartyEntity partyEntity = partyService.getPartyEntityById(partyId, caseReference);
        validateCanRemove(partyEntity, caseReference);

        partyEntity.setActive(YesOrNo.NO);
        partyEntity.getClaimPartyOrganisationList()
            .forEach(organisation -> organisation.setActive(YesOrNo.NO));
        partyRepository.save(partyEntity);

        ClaimEntity mainClaim = partyEntity.getPcsCase().getClaims().getFirst();
        String partyLabel = partyService.getPartyLabel(mainClaim, partyId);
        return new RemovedParty(partyService.getPartyName(partyEntity), partyService.getPartyRole(partyEntity),
                                partyLabel);
    }

    public void validateCanRemove(PartyEntity partyEntity, long caseReference) {
        PartyRole partyRole = partyService.getPartyRole(partyEntity);
        ClaimEntity mainClaim = partyEntity.getPcsCase().getClaims().getFirst();

        long partiesWithSameRole = mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == partyRole)
            .filter(claimParty -> partyService.isActive(claimParty.getParty()))
            .count();

        if (partiesWithSameRole <= 1) {
            throw new IllegalStateException(LAST_PARTY_ERROR);
        }

        UUID partyId = partyEntity.getId();
        boolean hasOpenGeneralApplication = genAppRepository.existsByPcsCaseCaseReferenceAndPartyIdAndStateIn(
            caseReference, partyId, OPEN_GEN_APP_STATES);
        boolean hasOpenCounterClaim = counterClaimRepository.existsByPcsCaseCaseReferenceAndPartyIdAndStatusIn(
            caseReference, partyId, OPEN_COUNTER_CLAIM_STATES);

        if (hasOpenGeneralApplication || hasOpenCounterClaim) {
            throw new IllegalStateException(OPEN_APPLICATION_OR_COUNTERCLAIM_ERROR);
        }
    }

    public boolean canSelectForRemoval(ClaimPartyEntity claimPartyEntity, ClaimEntity mainClaim) {
        PartyRole role = claimPartyEntity.getRole();
        return mainClaim.getClaimParties().stream()
            .filter(other -> other.getRole() == role)
            .filter(other -> partyService.isActive(other.getParty()))
            .count() > 1;
    }

    public record RemovedParty(String partyName, PartyRole partyRole, String partyLabel) {
    }
}
