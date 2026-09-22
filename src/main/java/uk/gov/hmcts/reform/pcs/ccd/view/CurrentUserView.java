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

/**
 * States which Group Access role the caller holds on this case, so consumers do not have to infer
 * it from IDAM roles.
 *
 * <p>Read-side counterpart of {@code CaseAccessGroupsUtil.deriveCaseAccessGroups}, from the same
 * inputs: the caller's rd-professional organisation and the case's party-to-organisation links.
 *
 * <p>Deliberately not read from RAS. Every user in a solicitor organisation holds
 * {@code claimant-solicitor} and {@code defendant-solicitor} alike - {@code GroupAccessType}
 * declares both with {@code accessMandatory} and {@code accessDefault} - so the role assignment
 * cannot say which side of a case they act on. Only the case can.
 */
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

        // The case-level party set, the same input CaseAccessGroupsUtil derives the case's access
        // groups from. Reading the main claim's parties instead would be a second source of truth
        // for who is on this case, and the two only coincide while a case has exactly one claim.
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

    /**
     * Only an active link counts. A representation ended by a later notice of change leaves an
     * inactive row behind, and treating that as current would keep the previous firm in the
     * defendant's journey.
     */
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
