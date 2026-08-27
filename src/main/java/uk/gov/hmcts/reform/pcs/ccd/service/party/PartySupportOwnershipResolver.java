package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

        if (isOwnedByUserOrganisation(partyEntity)) {
            return true;
        }

        return isRepresentedByUserOrganisation(partyEntity);
    }

    private boolean isOwnedByUserOrganisation(PartyEntity partyEntity) {
        String partyOrganisationId = partyEntity.getOrganisationId();

        return isNotBlank(partyOrganisationId)
            && partyOrganisationId.equals(organisationService.getOrganisationIdForCurrentUser());
    }

    /**
     * Representation is held against the organisation acting for the party rather than against an
     * individual representative, so a professional user acts for a party when their organisation holds a
     * current link to it. Links that have been ended are excluded.
     */
    private boolean isRepresentedByUserOrganisation(PartyEntity partyEntity) {
        List<OrganisationEntity> activeOrganisations =
            partyEntity.getClaimPartyOrganisationList().stream()
                .filter(claimPartyOrganisation -> YesOrNo.YES.equals(claimPartyOrganisation.getActive()))
                .map(ClaimPartyOrganisationEntity::getOrganisation)
                .filter(Objects::nonNull)
                .toList();

        if (activeOrganisations.isEmpty()) {
            return false;
        }

        String authenticatedOrganisationId = organisationService.getOrganisationIdForCurrentUser();

        return isNotBlank(authenticatedOrganisationId)
            && activeOrganisations.stream()
                .anyMatch(organisation ->
                              authenticatedOrganisationId.equals(organisation.getOrganisationId()));
    }
}
