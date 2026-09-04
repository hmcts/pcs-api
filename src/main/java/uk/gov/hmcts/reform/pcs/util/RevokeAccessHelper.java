package uk.gov.hmcts.reform.pcs.util;


import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.RoleAssignmentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.task.CaseRoleAssignmentTaskComponent;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class RevokeAccessHelper {

    private final ClaimPartyOrganisationRepository claimPartyOrganisationRepository;
    private final DraftCaseDataRepository draftCaseDataRepository;
    private final SchedulerClient schedulerClient;
    private final PartyAccessCodeRepository partyAccessCodeRepository;

    /**
     * 1. delete any drafts created by the existing LR's
     * 2. revoke access for the organisation LR's
     * - but only if the organisation does not represent any other defendant for the case
     * 3. deactivate the party legal representative organisation entities linked to the defendant to the LRO
     */
    public void withdrawOutgoingOrganisationsAccessToRespondToClaim(
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

        var claimPartyLegalRepOrgEntities = claimPartyOrganisationRepository
            .findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
                    defendantParty.getId(), organisationEntity.getId(), caseEntity.getCaseReference());

        if (!claimPartyLegalRepOrgEntities.isEmpty()) {
            claimPartyLegalRepOrgEntities.forEach(this::invalidatePartyLegalRepresentativeOrganisation);
            claimPartyOrganisationRepository.saveAll(claimPartyLegalRepOrgEntities);
        }
    }

    /**
     * 1. revoke the defendants role to the case
     * 2. delete the defendant's draft response to the claim
     * 3. invalidate the PIN
     */
    public void closeDefendantsSelfRepresentation(PcsCaseEntity caseEntity, PartyEntity defendantParty) {
        if (defendantParty.getIdamId() != null) {
            scheduleDefendantRoleRevocation(caseEntity.getCaseReference(), defendantParty.getIdamId().toString());
            draftCaseDataRepository.deleteByCaseReferenceAndEventIdAndIdamUserId(
                caseEntity.getCaseReference(), respondPossessionClaim,
                defendantParty.getIdamId()
            );
            log.info("Revoked access for defendant [{}] to respond to claim for case [{}]",
                      defendantParty.getId(), caseEntity.getCaseReference());
        }
        partyAccessCodeRepository.deleteByPcsCase_IdAndPartyId(caseEntity.getId(), defendantParty.getId());
        defendantParty.setIdamId(null);
    }

    private void scheduleDefendantRoleRevocation(long caseReference, String idamUserId) {
        schedulerClient.scheduleIfNotExists(
            CaseRoleAssignmentTaskComponent.ROLE_ASSIGNMENT_TASK_DESCRIPTOR
                .instance("revoke-defendant-%s-%s".formatted(caseReference, idamUserId))
                .data(RoleAssignmentTaskData.builder()
                          .caseReference(String.valueOf(caseReference))
                          .userId(idamUserId)
                          .role(UserRole.DEFENDANT)
                          .build())
                .scheduledTo(Instant.now())
        );
    }

    private void invalidatePartyLegalRepresentativeOrganisation(ClaimPartyOrganisationEntity partyLegalRepOrg) {
        partyLegalRepOrg.setActive(YesOrNo.NO);
        partyLegalRepOrg.setEndDate(Instant.now());
    }

}
