package uk.gov.hmcts.reform.pcs.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
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
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.time.Clock;
import java.time.Instant;
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
    private final AddressMapper addressMapper;
    private final CaseRoleAssignmentService caseRoleAssignmentService;
    private final NotificationService notificationService;
    private final Clock utcClock;

    public LegalRepresentativePartyLinkService(PcsCaseService pcsCaseService,
                                               OrganisationRepository
                                                   organisationRepository,
                                               ClaimPartyContactDetailsRepository
                                                   legalRepOrganisationContactDetailsRepository,
                                               OrganisationDetailsService organisationDetailsService,
                                               AddressMapper addressMapper,
                                               CaseRoleAssignmentService caseRoleAssignmentService,
                                               NotificationService notificationService,
                                               @Qualifier("utcClock") Clock utcClock) {
        this.pcsCaseService = pcsCaseService;
        this.organisationRepository = organisationRepository;
        this.legalRepOrganisationContactDetailsRepository = legalRepOrganisationContactDetailsRepository;
        this.organisationDetailsService = organisationDetailsService;
        this.addressMapper = addressMapper;
        this.caseRoleAssignmentService = caseRoleAssignmentService;
        this.notificationService = notificationService;
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

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

        unlinkExistingRepresentation(UUID.fromString(partyId));

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
        notificationService.sendNoticeOfChangeCompletedEmailNotification(defendantPartyEntity);
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

    private void unlinkExistingRepresentation(UUID partyId) {
        Optional<OrganisationEntity> partyLinkedToLegalRepresentativeOrganisationAndActive =
            organisationRepository
                .findByPartyLinkedToOrganisationAndActive(partyId);

        if (partyLinkedToLegalRepresentativeOrganisationAndActive.isPresent()) {
            OrganisationEntity existingLegalRepresentativeOrganisation =
                partyLinkedToLegalRepresentativeOrganisationAndActive.get();

            existingLegalRepresentativeOrganisation.getClaimPartyOrganisationList().stream()
                .filter(partyLegalRepresentativeOrganisation ->
                            partyLegalRepresentativeOrganisation.getParty().getId().equals(partyId))
                .forEach(this::invalidatePartyLegalRepresentativeOrganisation);

            organisationRepository.save(existingLegalRepresentativeOrganisation);
        }
    }

    private void invalidatePartyLegalRepresentativeOrganisation(ClaimPartyOrganisationEntity
                                                                    partyLegalRepOrg) {
        partyLegalRepOrg.setActive(YesOrNo.NO);
        partyLegalRepOrg.setEndDate(Instant.now());
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
