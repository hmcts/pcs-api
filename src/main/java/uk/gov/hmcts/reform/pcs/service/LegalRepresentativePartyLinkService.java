package uk.gov.hmcts.reform.pcs.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.ConflictOfInterestException;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.util.RevokeAccessHelper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class LegalRepresentativePartyLinkService {

    private final PcsCaseService pcsCaseService;
    private final OrganisationRepository organisationRepository;
    private final ClaimPartyContactDetailsRepository legalRepOrganisationContactDetailsRepository;
    private final OrganisationDetailsService organisationDetailsService;
    private final RevokeAccessHelper revokeAccessHelper;
    private final AddressMapper addressMapper;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final Clock utcClock;

    public LegalRepresentativePartyLinkService(PcsCaseService pcsCaseService,
                                               OrganisationRepository
                                                   organisationRepository,
                                               ClaimPartyContactDetailsRepository
                                                   legalRepOrganisationContactDetailsRepository,
                                               OrganisationDetailsService organisationDetailsService,
                                               AddressMapper addressMapper,
                                               CaseRoleAssignmentService caseRoleAssignmentService,
                                               RevokeAccessHelper revokeAccessHelper,
                                               @Qualifier("utcClock") Clock utcClock) {
        this.pcsCaseService = pcsCaseService;
        this.organisationRepository = organisationRepository;
        this.legalRepOrganisationContactDetailsRepository = legalRepOrganisationContactDetailsRepository;
        this.organisationDetailsService = organisationDetailsService;
        this.addressMapper = addressMapper;
        this.caseRoleAssignmentService = caseRoleAssignmentService;
        this.revokeAccessHelper = revokeAccessHelper;
        this.utcClock = utcClock;
    }

    // TODO: Retrieve actual organisation profile id from group access
    private static final String ORG_PROFILE_ID = "SOLICITOR_PROFILE";

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String partyId,
                                               OrganisationDetailsResponse organisationDetails) {
        String organisationId = organisationDetails.getOrganisationIdentifier();
        if (isAlreadyLinkedToParty(partyId, organisationId)) {
            throw new LegalRepresentativeAlreadyLinkedToPartyException(
                "Legal Representative or organisation already linked to Party [" + partyId + "]");
        }
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        this.checkConflictOfInterest(caseEntity, organisationDetails.getOrganisationIdentifier());

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

        unlinkExistingRepresentation(caseEntity, defendantPartyEntity);

        Optional<OrganisationEntity> legalRepresentativeOrganisationEntity =
            findExistingRepresentativeOrganisation(organisationId);

        OrganisationEntity legalRepresentativeOrganisation;

        if (legalRepresentativeOrganisationEntity.isPresent()) {

            legalRepresentativeOrganisation = legalRepresentativeOrganisationEntity.get();

            backfillOrganisationMetadata(legalRepresentativeOrganisation, organisationDetails);

            Optional<ClaimPartyContactDetailsEntity> existingContactDetails =
                legalRepOrganisationContactDetailsRepository
                    .findByOrganisationIdAndCaseReference(organisationId, caseReference);

            if (existingContactDetails.isEmpty()) {
                ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
                    buildLegalRepresentativeOrganisationContactDetails(caseEntity, legalRepresentativeOrganisation,
                                                                       organisationDetails);

                legalRepresentativeOrganisation
                    .addClaimPartyContactDetails(legalRepresentativeOrganisationContactDetails);

            }

        } else {
            legalRepresentativeOrganisation = createNewLegalRepresentative(
                organisationId,
                organisationDetails,
                caseEntity);
        }

        legalRepresentativeOrganisation.addParty(defendantPartyEntity);

        organisationRepository.save(legalRepresentativeOrganisation);
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

    private OrganisationEntity createNewLegalRepresentative(String id,
                                                            OrganisationDetailsResponse orgDetails,
                                                            PcsCaseEntity pcsCase) {

        OrganisationEntity legalRepresentativeOrganisation = OrganisationEntity
            .builder()
            .organisationId(id)
            .organisationName(orgDetails.getName())
            .organisationProfileId(ORG_PROFILE_ID)
            .createdDate(LocalDateTime.now(utcClock))
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            buildLegalRepresentativeOrganisationContactDetails(pcsCase, legalRepresentativeOrganisation, orgDetails);

        legalRepresentativeOrganisation
            .addClaimPartyContactDetails(legalRepresentativeOrganisationContactDetails);

        return legalRepresentativeOrganisation;
    }

    private boolean isAlreadyLinkedToParty(String partyId, String organisationId) {
        UUID targetPartyId = UUID.fromString(partyId);

        return organisationRepository
            .isOrganisationLinkedToPartyAndActive(organisationId, targetPartyId);
    }

    private Optional<OrganisationEntity> findExistingRepresentativeOrganisation(
        String organisationId) {
        return organisationRepository.findByOrganisationId(organisationId);
    }



    private void backfillOrganisationMetadata(OrganisationEntity legalRepresentativeOrganisation,
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

    private void unlinkExistingRepresentation(PcsCaseEntity caseEntity, PartyEntity defendantParty) {
        // 1. finds the active LegalRepresentativeOrganisationEntity for the defendants partyId and the case
        Optional<OrganisationEntity> partyLinkedToLegalRepresentativeOrganisationAndActive =
            organisationRepository
                .findByPartyLinkedToOrganisationAndCaseAndActive(
                    defendantParty.getId(), caseEntity.getCaseReference());

        // 2. if we have an LRO associated with this defendant for this case then revoke access
        partyLinkedToLegalRepresentativeOrganisationAndActive
            .ifPresent(legalRepresentativeOrganisation -> revokeAccessHelper.revokeOrganisationAccessToRespondToClaim(
                caseEntity,
                partyLinkedToLegalRepresentativeOrganisationAndActive.get(),
                defendantParty
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

    private ClaimPartyContactDetailsEntity buildLegalRepresentativeOrganisationContactDetails(
        PcsCaseEntity pcsCase,
        OrganisationEntity legalRepresentativeOrganisation,
        OrganisationDetailsResponse orgDetails) {

        return
            ClaimPartyContactDetailsEntity.builder()
                .pcsCase(pcsCase)
                .organisation(legalRepresentativeOrganisation)
                .address(addressMapper.toAddressEntityAndNormalise(
                    organisationDetailsService.getOrganisationAddress(orgDetails)))
                .build();
    }

}
