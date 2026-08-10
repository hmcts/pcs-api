package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
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

        return isRepresentedByUser(partyEntity, authenticatedUserId);
    }

    private boolean isRepresentedByUser(PartyEntity partyEntity, UUID authenticatedUserId) {
        List<LegalRepresentativeEntity> activeLegalRepresentatives =
            partyEntity.getClaimPartyLegalRepresentativeList().stream()
                .filter(claimPartyLegalRep -> YesOrNo.YES.equals(claimPartyLegalRep.getActive()))
                .map(ClaimPartyLegalRepresentativeEntity::getLegalRepresentative)
                .filter(Objects::nonNull)
                .toList();

        if (activeLegalRepresentatives.isEmpty()) {
            return false;
        }

        if (activeLegalRepresentatives.stream()
            .anyMatch(legalRepresentative -> authenticatedUserId.equals(legalRepresentative.getIdamId()))) {
            return true;
        }

        String authenticatedOrganisationId = organisationService.getOrganisationIdForCurrentUser();

        return isNotBlank(authenticatedOrganisationId)
            && activeLegalRepresentatives.stream()
                .anyMatch(legalRepresentative ->
                              authenticatedOrganisationId.equals(legalRepresentative.getOrganisationId()));
    }
}
