package uk.gov.hmcts.reform.pcs.util;


import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

@Component
@RequiredArgsConstructor
@Slf4j
public class RevokeAccessHelper {

    private final ClaimPartyLegalRepresentativeOrganisationRepository partyLegalRepOrgRepository;
    private final DraftCaseDataRepository draftCaseDataRepository;
    private final PartyAccessCodeRepository partyAccessCodeRepository;

    /**
     * 1. delete any drafts created by the existing LR's
     * 2. revoke access for the organisation LR's
     * - but only if the organisation does not represent any other defendant for the case
     * 3. deactivate the party legal representative organisation entities linked to the defendant to the LRO
     */
    public void revokeOrganisationAccessToRespondToClaim(
        PcsCaseEntity caseEntity,
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation,
        PartyEntity defendantParty,
        UserInfo user
    ) {
        this.draftCaseDataRepository.deleteByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
            caseEntity.getCaseReference(),
            EventId.respondPossessionClaim,
            String.valueOf(legalRepresentativeOrganisation.getOrganisationId()),
            defendantParty.getId()
        );

        partyLegalRepOrgRepository
            .findByPartyIdAndLegalRepresentativeOrganisation_OrganisationIdAndActive(
                defendantParty.getId(),
                legalRepresentativeOrganisation.getOrganisationId(),
                YesOrNo.YES
            ).ifPresent(partyLegalRepOrg -> {
                invalidatePartyLegalRepresentativeOrganisation(partyLegalRepOrg);
                partyLegalRepOrgRepository.save(partyLegalRepOrg);
            });
    }

    /**
     * 1. revoke the defendants role to the case
     * 2. delete the defendant's draft response to the claim
     * 3. invalidate the PIN
     */
    public void revokeDefendantsAccessToRespondToClaim(PcsCaseEntity caseEntity, PartyEntity defendantParty) {
        if (defendantParty.getIdamId() != null) {
            draftCaseDataRepository.deleteByCaseReferenceAndEventIdAndIdamUserId(
                caseEntity.getCaseReference(), EventId.respondPossessionClaim, defendantParty.getIdamId());
            log.debug(
                "Revoked access for defendant [{}] to respond to claim for case [{}]",
                defendantParty.getId(), caseEntity.getCaseReference()
            );
        }
        partyAccessCodeRepository.deleteByPcsCase_IdAndPartyId(caseEntity.getId(), defendantParty.getId());
        defendantParty.setIdamId(null);
    }

    private void invalidatePartyLegalRepresentativeOrganisation(
        ClaimPartyLegalRepresentativeOrganisationEntity legalRepOrganisation) {
        legalRepOrganisation.setActive(YesOrNo.NO);
        legalRepOrganisation.setEndDate(Instant.now());
    }
}
