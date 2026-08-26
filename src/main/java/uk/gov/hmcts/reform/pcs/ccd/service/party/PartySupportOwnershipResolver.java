package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@AllArgsConstructor
public class PartySupportOwnershipResolver {

    private final OrganisationService organisationService;

    public Set<UUID> resolveRepresentedPartyIds(Collection<PartyEntity> partyEntities, UUID authenticatedUserId) {
        if (authenticatedUserId == null) {
            return Set.of();
        }

        Supplier<String> authenticatedOrganisationId = new SingleLookupOrganisationIdSupplier();

        return partyEntities.stream()
            .filter(partyEntity -> isOwnedByUser(partyEntity, authenticatedUserId, authenticatedOrganisationId))
            .map(PartyEntity::getId)
            .collect(Collectors.toSet());
    }

    private boolean isOwnedByUser(PartyEntity partyEntity, UUID authenticatedUserId,
                                  Supplier<String> authenticatedOrganisationId) {
        if (authenticatedUserId.equals(partyEntity.getIdamId())) {
            return true;
        }

        if (isOwnedByUserOrganisation(partyEntity, authenticatedOrganisationId)) {
            return true;
        }

        return isRepresentedByUserOrganisation(partyEntity, authenticatedOrganisationId);
    }

    private boolean isOwnedByUserOrganisation(PartyEntity partyEntity,
                                              Supplier<String> authenticatedOrganisationId) {
        String partyOrganisationId = partyEntity.getOrganisationId();

        return isNotBlank(partyOrganisationId)
            && partyOrganisationId.equals(authenticatedOrganisationId.get());
    }

    /**
     * Representation is held against the organisation acting for the party rather than against an
     * individual representative, so a professional user acts for a party when their organisation holds a
     * current link to it. Links that have been ended are excluded.
     */
    private boolean isRepresentedByUserOrganisation(PartyEntity partyEntity,
                                                    Supplier<String> authenticatedOrganisationId) {
        List<OrganisationEntity> activeOrganisations =
            partyEntity.getClaimPartyOrganisationList().stream()
                .filter(claimPartyOrganisation -> YesOrNo.YES.equals(claimPartyOrganisation.getActive()))
                .map(ClaimPartyOrganisationEntity::getOrganisation)
                .filter(Objects::nonNull)
                .toList();

        if (activeOrganisations.isEmpty()) {
            return false;
        }

        String organisationId = authenticatedOrganisationId.get();

        return isNotBlank(organisationId)
            && activeOrganisations.stream()
                .anyMatch(organisation -> organisationId.equals(organisation.getOrganisationId()));
    }

    private final class SingleLookupOrganisationIdSupplier implements Supplier<String> {

        private boolean resolved;
        private String organisationId;

        @Override
        public String get() {
            if (!resolved) {
                organisationId = organisationService.getOrganisationIdForCurrentUser();
                resolved = true;
            }

            return organisationId;
        }
    }
}
