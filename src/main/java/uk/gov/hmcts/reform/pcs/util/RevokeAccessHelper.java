package uk.gov.hmcts.reform.pcs.util;


import static java.lang.String.valueOf;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class RevokeAccessHelper {

    private final ClaimPartyLegalRepresentativeOrganisationRepository claimPartyLegalRepOrgRepository;
    private final DraftCaseDataRepository draftCaseDataRepository;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final PartyAccessCodeRepository partyAccessCodeRepository;

    /**
     * 1. delete any drafts created by the existing LR's
     * 2. revoke access for the organisation LR's
     * - but only if the organisation does not represent any other defendant for the case
     * 3. deactivate the party legal representative organisation entities linked to the defendant to the LRO
     */
    public void revokeOrgAccessToRespondToClaim(PcsCaseEntity caseEntity,
                                                LegalRepresentativeOrganisationEntity legalRepOrg,
                                                PartyEntity defendantParty) {

        this.draftCaseDataRepository.deleteByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
                caseEntity.getCaseReference(),
                respondPossessionClaim,
                valueOf(legalRepOrg.getOrganisationId()),
                defendantParty.getId()
        );

        var claimPartyLegalRepOrgEntities = claimPartyLegalRepOrgRepository
            .findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
                    defendantParty.getId(), legalRepOrg.getId(), caseEntity.getCaseReference());

        if (!claimPartyLegalRepOrgEntities.isEmpty()) {
            claimPartyLegalRepOrgEntities.forEach(this::invalidatePartyLegalRepresentativeOrganisation);
            claimPartyLegalRepOrgRepository.saveAll(claimPartyLegalRepOrgEntities);
        }
    }

    /**
     * 1. revoke the defendants role to the case
     * 2. delete the defendant's draft response to the claim
     * 3. invalidate the PIN
     */
    public void revokeDefendantsAccessToRespondToClaim(PcsCaseEntity caseEntity, PartyEntity defendantParty) {
        if (defendantParty.getIdamId() != null) {
            caseRoleAssignmentService.revokeCaseRole(
                caseEntity.getCaseReference(), defendantParty.getIdamId().toString(), UserRole.DEFENDANT);
            draftCaseDataRepository.deleteByCaseReferenceAndEventIdAndIdamUserId(
                caseEntity.getCaseReference(), respondPossessionClaim, defendantParty.getIdamId());
            log.debug("Revoked access for defendant [{}] to respond to claim for case [{}]",
                      defendantParty.getId(), caseEntity.getCaseReference());
        }
        partyAccessCodeRepository.deleteByPcsCase_IdAndPartyId(caseEntity.getId(), defendantParty.getId());
        defendantParty.setIdamId(null);
    }

    private void invalidatePartyLegalRepresentativeOrganisation(ClaimPartyLegalRepresentativeOrganisationEntity
                                                                    partyLegalRepOrg) {
        partyLegalRepOrg.setActive(YesOrNo.NO);
        partyLegalRepOrg.setEndDate(Instant.now());
    }

}
