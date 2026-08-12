package uk.gov.hmcts.reform.pcs.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.ConflictOfInterestException;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetails;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.util.RevokeAccessHelper;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalRepresentativePartyLinkService {

    private final PcsCaseService pcsCaseService;
    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    private final OrganisationDetailsService organisationDetailsService;
    private final RevokeAccessHelper revokeAccessHelper;
    private final AddressMapper addressMapper;
    private final CaseRoleAssignmentService caseRoleAssignmentService;

    // TODO: Retrieve actual organisation profile id from group access
    private static final String ORG_PROFILE_ID = "SOLICITOR_PROFILE";

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String partyId, UserInfo user,
                                               OrganisationDetailsResponse organisationDetails) {
        String organisationId = organisationDetails.getOrganisationIdentifier();
        if (isAlreadyLinkedToParty(partyId, organisationId)) {
            throw new LegalRepresentativeAlreadyLinkedToPartyException(
                "Legal Representative or organisation already linked to Party [" + partyId + "]");
        }
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        this.checkConflictOfInterest(caseEntity, organisationDetails.organisationIdentifier());

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

        unlinkExistingRepresentation(caseEntity, defendantPartyEntity, user);

        Optional<LegalRepresentativeOrganisationEntity> legalRepresentativeOrganisationEntity =
            findExistingRepresentativeOrganisation(organisationId, caseReference);

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation;

        if (legalRepresentativeOrganisationEntity.isPresent()) {

            legalRepresentativeOrganisation = legalRepresentativeOrganisationEntity.get();

            backfillOrganisationMetadata(legalRepresentativeOrganisation, organisationDetails);
        } else {
            legalRepresentativeOrganisation = createNewLegalRepresentative(
                organisationId,
                organisationDetails,
                caseEntity);
        }

        legalRepresentativeOrganisation.addParty(defendantPartyEntity);

        legalRepresentativeOrganisationRepository.save(legalRepresentativeOrganisation);
        revokeDefendantAccessForRepresentedParty(caseReference, defendantPartyEntity);
    }

    private void revokeDefendantAccessForRepresentedParty(long caseReference, PartyEntity defendantPartyEntity) {
        if (defendantPartyEntity.getIdamId() == null) {
            return;
        }

        caseRoleAssignmentService.revokeRasRole(
            caseReference,
            defendantPartyEntity.getIdamId().toString(),
            UserRole.DEFENDANT
        );
    }

    private LegalRepresentativeOrganisationEntity createNewLegalRepresentative(String id,
                                                                               OrganisationDetailsResponse orgDetails,
                                                                               PcsCaseEntity pcsCase) {

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation = LegalRepresentativeOrganisationEntity
            .builder()
            .organisationId(id)
            .organisationName(orgDetails.getName())
            .organisationProfileId(ORG_PROFILE_ID)
            .build();

        LegalRepresentativeOrganisationContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            LegalRepresentativeOrganisationContactDetailsEntity.builder()
                .pcsCase(pcsCase)
                .legalRepresentativeOrganisation(legalRepresentativeOrganisation)
                .address(addressMapper.toAddressEntityAndNormalise(
                    organisationDetailsService.getOrganisationAddress(orgDetails)))
                .build();

        legalRepresentativeOrganisation
            .setLegalRepresentativeOrganisationContactDetails(legalRepresentativeOrganisationContactDetails);

        return legalRepresentativeOrganisation;
    }

    private boolean isAlreadyLinkedToParty(String partyId, String organisationId) {
        UUID targetPartyId = UUID.fromString(partyId);

        return legalRepresentativeOrganisationRepository
            .isRepresentativeOrganisationLinkedToPartyAndActive(organisationId, targetPartyId);
    }

    private Optional<LegalRepresentativeOrganisationEntity> findExistingRepresentativeOrganisation(
        String organisationId, long caseReference) {
        return legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(organisationId,
                                                                                              caseReference);
    }

    private void backfillOrganisationMetadata(LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation,
                                              OrganisationDetailsResponse organisationDetails) {
        if (legalRepresentativeOrganisation.getOrganisationId() == null) {
            legalRepresentativeOrganisation.setOrganisationId(organisationDetails.getOrganisationIdentifier());
        }
        if (legalRepresentativeOrganisation.getOrganisationName() == null) {
            legalRepresentativeOrganisation.setOrganisationName(organisationDetails.getName());
        }
    }

    private PartyEntity getDefendantPartyEntity(PcsCaseEntity caseEntity, String partyId) {
        return caseEntity.getClaims().getFirst()
            .getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .filter(partyEntity -> partyEntity.getId().equals(UUID.fromString(partyId)))
            .findFirst()
            .orElseThrow(() -> {
                log.error("Unable to find Party [{}]", partyId);
                return new PartyNotFoundException("Unable to find Party with Id [" + partyId + "]");
            });
    }

    private void unlinkExistingRepresentation(PcsCaseEntity caseEntity, PartyEntity defendantParty, UserInfo user) {
        // 1. finds the active LegalRepresentativeOrganisationEntity for the defendants partyId and the case
        Optional<LegalRepresentativeOrganisationEntity> legalRepresentativeOrganisationEntity =
            legalRepresentativeOrganisationRepository
                .findByPartyLinkedToLegalRepresentativeOrganisationAndCaseAndActive(
                    defendantParty.getId(), caseEntity.getCaseReference());

        // 2. if we have an LRO associated with this defendant for this case then revoke access
        legalRepresentativeOrganisationEntity
            .ifPresent(legalRepresentativeOrganisation -> revokeAccessHelper.revokeOrganisationAccessToRespondToClaim(
                caseEntity,
                legalRepresentativeOrganisation,
                defendantParty,
                user
            ));

        // 3. revoke defendants access
        revokeAccessHelper.revokeDefendantsAccessToRespondToClaim(caseEntity, defendantParty);
    }

    private void checkConflictOfInterest(PcsCaseEntity caseEntity, String organisationId) {
        PartyEntity claimant = caseEntity.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> PartyRole.CLAIMANT.equals(claimParty.getRole()))
            .map(ClaimPartyEntity::getParty)
            .findFirst()
            .orElseThrow(() -> {
                log.error("Unable to find claimant Party");
                return new PartyNotFoundException("Unable to find claimant Party");
            });

        if (organisationId.equals(claimant.getOrganisationId())) {
            throw new ConflictOfInterestException(
                "Organisation cannot represent both claimant and defendant in the same case");
        }
    }

}
