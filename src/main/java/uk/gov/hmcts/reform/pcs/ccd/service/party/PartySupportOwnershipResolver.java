package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.Objects;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@AllArgsConstructor
public class PartySupportOwnershipResolver {

    private final OrganisationDetailsService organisationDetailsService;

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
        if (partyEntity.getClaimPartyLegalRepresentativeList() == null) {
            return false;
        }

        String authenticatedOrganisationId =
            organisationDetailsService.getOrganisationIdentifier(authenticatedUserId.toString());

        return partyEntity.getClaimPartyLegalRepresentativeList().stream()
            .filter(claimPartyLegalRep -> YesOrNo.YES.equals(claimPartyLegalRep.getActive()))
            .map(ClaimPartyLegalRepresentativeEntity::getLegalRepresentative)
            .filter(Objects::nonNull)
            .anyMatch(legalRepresentative ->
                          isUserOrOrganisationMatch(legalRepresentative, authenticatedUserId,
                                                    authenticatedOrganisationId));
    }

    private boolean isUserOrOrganisationMatch(LegalRepresentativeEntity legalRepresentative,
                                              UUID authenticatedUserId,
                                              String authenticatedOrganisationId) {
        return authenticatedUserId.equals(legalRepresentative.getIdamId())
            || (isNotBlank(authenticatedOrganisationId)
            && authenticatedOrganisationId.equals(legalRepresentative.getOrganisationId()));
    }
}
