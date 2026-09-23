package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toCollection;

@Component
@AllArgsConstructor
public class DefendantSupportEligibilityResolver {

    private final PartySupportOwnershipResolver partySupportOwnershipResolver;

    public Set<UUID> resolveEligibleDefendantPartyIds(PcsCaseEntity pcsCaseEntity, UUID authenticatedUserId) {
        if (pcsCaseEntity == null || authenticatedUserId == null) {
            return Set.of();
        }

        Set<UUID> defendantPartyIds = defendantPartyIds(pcsCaseEntity);
        if (defendantPartyIds.isEmpty()) {
            return Set.of();
        }

        Set<UUID> representedPartyIds = partySupportOwnershipResolver
            .resolveRepresentedPartyIds(pcsCaseEntity.getParties(), authenticatedUserId);

        return defendantPartyIds.stream()
            .filter(representedPartyIds::contains)
            .collect(toCollection(LinkedHashSet::new));
    }

    private Set<UUID> defendantPartyIds(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (CollectionUtils.isEmpty(claims)) {
            return Set.of();
        }

        return claims.getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(this::partyId)
            .filter(Objects::nonNull)
            .collect(toCollection(LinkedHashSet::new));
    }

    private UUID partyId(ClaimPartyEntity claimParty) {
        if (claimParty.getId() != null && claimParty.getId().getPartyId() != null) {
            return claimParty.getId().getPartyId();
        }

        PartyEntity party = claimParty.getParty();
        return party == null ? null : party.getId();
    }
}
