package uk.gov.hmcts.reform.pcs.util;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.PartyLegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RevokeAccessHelper {

    private final PartyLegalRepresentativeOrganisationRepository partyLegalRepresentativeOrganisationRepository;
    private final DraftCaseDataRepository draftCaseDataRepository;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final PartyAccessCodeRepository partyAccessCodeRepository;

    /**
     * 1. delete any drafts created by the existing LR's
     * 2. revoke access for the legal representatives for the Organisation
     * 3. delete this legal representatives draft response to the claim
     * 4. deactivate the party legal representative organisation entities linked to the defendant to the LRO
     */
    public void revokeOrganisationAccessToRespondToClaim(
        PcsCaseEntity caseEntity,
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation,
        PartyEntity defendantParty
    ) {
        this.draftCaseDataRepository.deleteByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
            caseEntity.getCaseReference(),
            EventId.respondPossessionClaim,
            String.valueOf(legalRepresentativeOrganisation.getId()), defendantParty.getId()
        );
        /*
         * revoke access for the legal representatives for the Organisation
         * AND delete this legal representatives draft response to the claim
         * - but only if the organisation does not represent any other defendant for the case
         */
        boolean representsOtherDefendantsForCase = this.representsOtherDefendantsForCase(
            legalRepresentativeOrganisation,
            caseEntity.getCaseReference(),
            defendantParty.getId().toString()
        );
        if (!representsOtherDefendantsForCase) {
            Set<UUID> legalRepresentativeIds = legalRepresentativeOrganisation.getLegalRepresentativeList().stream()
                .map(LegalRepresentativeEntity::getIdamId)
                    .collect(Collectors.toSet());
            legalRepresentativeIds.forEach(idamId -> caseRoleAssignmentService.revokeRasRole(
                    caseEntity.getCaseReference(),
                    String.valueOf(idamId), UserRole.DEFENDANT_SOLICITOR
                ));
            log.debug("Revoked access for legal representatives [{}] to respond to claim for case [{}]",
                      legalRepresentativeIds, caseEntity.getCaseReference());
        }

        // deactivate the party legal representative organisation entities linked to the defendant to the LRO
        List<PartyLegalRepresentativeOrganisationEntity> partyLegalRepresentativeOrganisationEntities =
            partyLegalRepresentativeOrganisationRepository
                .findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
                    defendantParty.getId(),
                    legalRepresentativeOrganisation.getId(),
                    caseEntity.getCaseReference()
                );

        if (!partyLegalRepresentativeOrganisationEntities.isEmpty()) {
            partyLegalRepresentativeOrganisationEntities.forEach(this::invalidatePartyLegalRepresentativeOrganisation);
            partyLegalRepresentativeOrganisationRepository.saveAll(partyLegalRepresentativeOrganisationEntities);
        }
    }

    /**
     * 1. revoke the defendants role to the case
     *         2. delete the defendant's draft response to the claim
     *         3. if the defendant has not started a response
     *         a) revoke the defendants access to the response to the claim
     *         b) Invalidate the PIN
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
    }

    private boolean representsOtherDefendantsForCase(
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation,
        long caseReference,
        String defendantPartyId
    ) {
        UUID excludedPartyId = UUID.fromString(defendantPartyId);
        long count = partyLegalRepresentativeOrganisationRepository.countOtherDefendantsRepresentedByOrganisation(
            legalRepresentativeOrganisation.getId(),
            caseReference,
            excludedPartyId,
            PartyRole.DEFENDANT
        );

        return count > 0;
    }

    private void invalidatePartyLegalRepresentativeOrganisation(PartyLegalRepresentativeOrganisationEntity
                                                                    partyLegalRepOrg) {
        partyLegalRepOrg.setActive(YesOrNo.NO);
        partyLegalRepOrg.setEndDate(Instant.now());
    }

}
