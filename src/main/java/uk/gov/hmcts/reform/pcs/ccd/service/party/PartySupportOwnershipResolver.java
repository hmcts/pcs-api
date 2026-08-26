package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@AllArgsConstructor
public class PartySupportOwnershipResolver {

    private final OrganisationService organisationService;

    public boolean isOwnedByUser(PartyEntity partyEntity, UUID authenticatedUserId) {
        if (authenticatedUserId == null || partyEntity == null) {
            return false;
        }

        if (authenticatedUserId.equals(partyEntity.getIdamId())) {
            return true;
        }

        if (!requiresOrganisationLookup(partyEntity)) {
            return false;
        }

        String authenticatedOrganisationId = organisationService.getOrganisationIdForCurrentUser();

        return isOwnedByUser(partyEntity, authenticatedUserId, authenticatedOrganisationId);
    }

    private boolean isOwnedByUser(PartyEntity partyEntity,
                                  UUID authenticatedUserId,
                                  String authenticatedOrganisationId) {
        if (authenticatedUserId.equals(partyEntity.getIdamId())) {
            return true;
        }

        return isOwnedByUserOrganisation(partyEntity, authenticatedOrganisationId)
            || isRepresentedByUserOrganisation(partyEntity, authenticatedOrganisationId);
    }

    public Set<UUID> resolveRepresentedPartyIds(Collection<PartyEntity> partyEntities, UUID authenticatedUserId) {
        if (partyEntities == null || partyEntities.isEmpty() || authenticatedUserId == null) {
            return Set.of();
        }

        boolean organisationLookupRequired = partyEntities.stream()
            .filter(Objects::nonNull)
            .filter(partyEntity -> !authenticatedUserId.equals(partyEntity.getIdamId()))
            .anyMatch(this::requiresOrganisationLookup);

        String authenticatedOrganisationId = organisationLookupRequired
            ? organisationService.getOrganisationIdForCurrentUser()
            : null;

        return partyEntities.stream()
            .filter(Objects::nonNull)
            .filter(partyEntity -> isOwnedByUser(
                partyEntity,
                authenticatedUserId,
                authenticatedOrganisationId
            ))
            .map(PartyEntity::getId)
            .filter(Objects::nonNull)
            .collect(toCollection(LinkedHashSet::new));
    }

    private boolean requiresOrganisationLookup(PartyEntity partyEntity) {
        if (isNotBlank(partyEntity.getOrganisationId())) {
            return true;
        }

        return partyEntity.getClaimPartyOrganisationList().stream()
            .anyMatch(claimPartyOrganisation ->
                          YesOrNo.YES.equals(claimPartyOrganisation.getActive())
                              && claimPartyOrganisation.getOrganisation() != null);
    }

    private boolean isOwnedByUserOrganisation(PartyEntity partyEntity,
                                              String authenticatedOrganisationId) {
        String partyOrganisationId = partyEntity.getOrganisationId();

        return isNotBlank(partyOrganisationId)
            && partyOrganisationId.equals(authenticatedOrganisationId);
    }

    private boolean isRepresentedByUserOrganisation(PartyEntity partyEntity,
                                                    String authenticatedOrganisationId) {
        List<OrganisationEntity> activeOrganisations =
            partyEntity.getClaimPartyOrganisationList().stream()
                .filter(claimPartyOrganisation -> YesOrNo.YES.equals(claimPartyOrganisation.getActive()))
                .map(ClaimPartyOrganisationEntity::getOrganisation)
                .filter(Objects::nonNull)
                .toList();

        if (activeOrganisations.isEmpty()) {
            return false;
        }

        return isNotBlank(authenticatedOrganisationId)
            && activeOrganisations.stream()
            .anyMatch(organisation ->
                          authenticatedOrganisationId.equals(organisation.getOrganisationId()));
    }
}
