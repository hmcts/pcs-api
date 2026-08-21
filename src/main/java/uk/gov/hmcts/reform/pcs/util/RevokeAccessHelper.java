package uk.gov.hmcts.reform.pcs.util;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RevokeAccessHelper {

    private final ClaimPartyOrganisationRepository claimPartyOrganisationRepository;
    private final DraftCaseDataRepository draftCaseDataRepository;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final PartyAccessCodeRepository partyAccessCodeRepository;

    /**
     * 1. delete any drafts created by the existing LR's
     * 2. revoke access for the organisation LR's
     * - but only if the organisation does not represent any other defendant for the case
     * 3. deactivate the party legal representative organisation entities linked to the defendant to the LRO
     */
    public void revokeOrganisationAccessToRespondToClaim(
        PcsCaseEntity caseEntity,
        OrganisationEntity organisationEntity,
        PartyEntity defendantParty
    ) {
        this.draftCaseDataRepository.deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
            caseEntity.getCaseReference(),
            EventId.respondPossessionClaim,
            String.valueOf(organisationEntity.getOrganisationId()),
            defendantParty.getId()
        );

        boolean representsOtherDefendantsForCase = this.representsOtherDefendantsForCase(
            organisationEntity,
            caseEntity.getCaseReference(),
            defendantParty.getId().toString()
        );
        if (!representsOtherDefendantsForCase) {
            /* TODO
            Set<UUID> legalRepresentativeIds = organisationEntity.getLegalRepresentativeList().stream()
                .filter(lr -> lr.getIdamId().toString() != user.getUid())
                .map(LegalRepresentativeEntity::getIdamId)
                    .collect(Collectors.toSet());
            legalRepresentativeIds.forEach(idamId -> caseRoleAssignmentService.revokeRasRole(
                    caseEntity.getCaseReference(),
                    String.valueOf(idamId), UserRole.DEFENDANT_SOLICITOR
                ));
            log.debug("Revoked access for legal representatives [{}] to respond to claim for case [{}]",
                      legalRepresentativeIds, caseEntity.getCaseReference());

             */
        }

        List<ClaimPartyOrganisationEntity> claimPartyOrganisationEntities =
            claimPartyOrganisationRepository
                .findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
                    defendantParty.getId(),
                    organisationEntity.getId(),
                    caseEntity.getCaseReference()
                );
        if (!claimPartyOrganisationEntities.isEmpty()) {
            claimPartyOrganisationEntities.forEach(this::invalidatePartyLegalRepresentativeOrganisation);
            claimPartyOrganisationRepository.saveAll(claimPartyOrganisationEntities);
        }
    }

    /**
     * 1. revoke the defendants role to the case
     * 2. delete the defendant's draft response to the claim
     * 3. invalidate the PIN
     */
    public void revokeDefendantsAccessToRespondToClaim(PcsCaseEntity caseEntity, PartyEntity defendantParty) {
        if (defendantParty.getIdamId() != null) {
            caseRoleAssignmentService.revokeRasRole(
                caseEntity.getCaseReference(), defendantParty.getIdamId().toString(), UserRole.DEFENDANT);
            draftCaseDataRepository.deleteByCaseReferenceAndEventIdAndIdamUserId(
                caseEntity.getCaseReference(), EventId.respondPossessionClaim, defendantParty.getIdamId());
            log.debug("Revoked access for defendant [{}] to respond to claim for case [{}]",
                      defendantParty.getId(), caseEntity.getCaseReference());
        }
        partyAccessCodeRepository.deleteByPcsCase_IdAndPartyId(caseEntity.getId(), defendantParty.getId());
        defendantParty.setIdamId(null);
    }

    private boolean representsOtherDefendantsForCase(
        OrganisationEntity organisationEntity,
        long caseReference,
        String defendantPartyId
    ) {
        UUID excludedPartyId = UUID.fromString(defendantPartyId);
        long count = claimPartyOrganisationRepository.countOtherDefendantsRepresentedByOrganisation(
            organisationEntity.getId(),
            caseReference,
            excludedPartyId,
            PartyRole.DEFENDANT
        );

        return count > 0;
    }

    private void invalidatePartyLegalRepresentativeOrganisation(ClaimPartyOrganisationEntity
                                                                    claimPartyOrganisationEntity) {
        claimPartyOrganisationEntity.setActive(YesOrNo.NO);
        claimPartyOrganisationEntity.setEndDate(Instant.now());
    }

}
