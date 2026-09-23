package uk.gov.hmcts.reform.pcs.ccd.view;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.GroupAccessType.groupRoleFor;
import static uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole.DEFENDANT;

import java.util.Collection;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

/** Caller's group role on the case. RAS cannot tell claimant-solicitor from defendant-solicitor. */
@Component
@AllArgsConstructor
public class CurrentUserView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity, String organisationId) {
        pcsCase.setCurrentUserGroupRole(groupRole(pcsCaseEntity, organisationId).orElse(null));
    }

    private Optional<String> groupRole(PcsCaseEntity pcsCaseEntity, String organisationId) {
        if (organisationId == null) {
            return Optional.empty();
        }

        Collection<PartyEntity> parties = pcsCaseEntity.getParties();

        return claimantRole(parties, organisationId)
            .or(() -> defendantSolicitorRole(parties, organisationId));
    }

    private Optional<String> claimantRole(Collection<PartyEntity> parties, String organisationId) {
        return parties.stream()
            .filter(party -> party.isClaimCreator() && organisationId.equals(party.getOrganisationId()))
            .findFirst()
            .flatMap(party -> groupRoleFor(party.getOrganisationProfileId(), CLAIMANT));
    }

    /** Active representation only; an ended NoC link must not keep the previous firm on the journey. */
    private Optional<String> defendantSolicitorRole(Collection<PartyEntity> parties, String organisationId) {
        return parties.stream()
            .flatMap(party -> party.getClaimPartyOrganisationList().stream())
            .filter(link -> YesOrNo.YES == link.getActive())
            .map(ClaimPartyOrganisationEntity::getOrganisation)
            .filter(organisation -> organisationId.equals(organisation.getOrganisationId()))
            .findFirst()
            .flatMap(organisation -> groupRoleFor(organisation.getOrganisationProfileId(), DEFENDANT));
    }
}
