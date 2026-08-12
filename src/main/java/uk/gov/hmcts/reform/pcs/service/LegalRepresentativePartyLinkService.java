package uk.gov.hmcts.reform.pcs.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetails;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalRepresentativePartyLinkService {

    private final PcsCaseService pcsCaseService;
    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    private final OrganisationService organisationService;
    private final AddressMapper addressMapper;
    private final CaseRoleAssignmentService caseRoleAssignmentService;

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String partyId, UserInfo user) {
        OrganisationDetails organisationDetails = organisationService.getOrganisationDetails(user.getUid());
        if (isAlreadyLinkedToParty(partyId, organisationDetails.organisationIdentifier())) {
            throw new LegalRepresentativeAlreadyLinkedToPartyException(
                "Legal Representative or organisation already linked to Party [" + partyId + "]");
        }
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

        unlinkExistingRepresentation(UUID.fromString(partyId));

        Optional<LegalRepresentativeOrganisationEntity> legalRepresentativeOrganisationEntity =
            findExistingRepresentativeOrganisation(organisationDetails.organisationIdentifier(), caseReference);

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation;

        if (legalRepresentativeOrganisationEntity.isPresent()) {
            legalRepresentativeOrganisation = legalRepresentativeOrganisationEntity.get();
            backfillOrganisationMetadata(legalRepresentativeOrganisation, organisationDetails);
            // was calling backfillLegalRepresentative(idamId) here , which
            // got removed in 5794 so that path no longer compiles. leaving a "breadcrumb"
            // in case we still need to hang idam users off the org somehow
        } else {
            legalRepresentativeOrganisation = createNewLegalRepresentative(
                organisationDetails.organisationIdentifier(),
                organisationDetails);
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

    // Was used to create a LegalRepresentativeEntity + addLegalRepresentativeOrganisation
    // on the case, but those methods/types aren't there after the 5794 merge
    private LegalRepresentativeOrganisationEntity createNewLegalRepresentative(String organisationId,
                                                                               OrganisationDetails organisationDetails) {
        // TODO jordan: double-check we don't still need to attach this to the case somehow
        return LegalRepresentativeOrganisationEntity.builder()
            .organisationId(organisationId)
            .organisationName(organisationDetails.name())
            .address(addressMapper.toAddressEntityAndNormalise(organisationDetails.address()))
            .build();
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
                                              OrganisationDetails organisationDetails) {
        if (legalRepresentativeOrganisation.getOrganisationId() == null) {
            legalRepresentativeOrganisation.setOrganisationId(organisationDetails.organisationIdentifier());
        }
        if (legalRepresentativeOrganisation.getOrganisationName() == null) {
            legalRepresentativeOrganisation.setOrganisationName(organisationDetails.name());
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
        Optional<LegalRepresentativeOrganisationEntity> partyLinkedToLegalRepresentativeOrganisationAndActive =
            legalRepresentativeOrganisationRepository
                .findByPartyLinkedToLegalRepresentativeOrganisationAndActive(partyId);

        if (partyLinkedToLegalRepresentativeOrganisationAndActive.isPresent()) {
            LegalRepresentativeOrganisationEntity existingLegalRepresentativeOrganisation =
                partyLinkedToLegalRepresentativeOrganisationAndActive.get();

            existingLegalRepresentativeOrganisation.getClaimPartyLegalRepresentativeOrganisationList().stream()
                .filter(partyLegalRepresentativeOrganisation ->
                            partyLegalRepresentativeOrganisation.getParty().getId().equals(partyId))
                .forEach(this::invalidatePartyLegalRepresentativeOrganisation);

            legalRepresentativeOrganisationRepository.save(existingLegalRepresentativeOrganisation);
        }
    }

    private void invalidatePartyLegalRepresentativeOrganisation(ClaimPartyLegalRepresentativeOrganisationEntity
                                                                    partyLegalRepOrg) {
        partyLegalRepOrg.setActive(YesOrNo.NO);
        partyLegalRepOrg.setEndDate(Instant.now());
    }

}
