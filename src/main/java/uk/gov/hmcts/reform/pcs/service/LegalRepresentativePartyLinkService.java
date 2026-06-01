package uk.gov.hmcts.reform.pcs.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalRepresentativePartyLinkService {

    private final PcsCaseService pcsCaseService;
    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    private final OrganisationDetailsService organisationDetailsService;
    private final AddressMapper addressMapper;

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String partyId, UserInfo user,
                                               OrganisationDetailsResponse organisationDetails) {
        String organisationId = organisationDetails.getOrganisationIdentifier();
        if (isAlreadyLinkedToParty(partyId, organisationId)) {
            throw new LegalRepresentativeAlreadyLinkedToPartyException(
                "Legal Representative or organisation already linked to Party [" + partyId + "]");
        }
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

        unlinkExistingRepresentation(UUID.fromString(partyId));

        UUID idamId = UUID.fromString(user.getUid());

        Optional<LegalRepresentativeOrganisationEntity> legalRepresentativeOrganisationEntity =
            findExistingRepresentativeOrganisation(organisationId, caseReference);

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation;

        if (legalRepresentativeOrganisationEntity.isPresent()) {

            legalRepresentativeOrganisation = legalRepresentativeOrganisationEntity.get();

            backfillOrganisationMetadata(legalRepresentativeOrganisation, organisationDetails);
            backfillLegalRepresentative(legalRepresentativeOrganisation, idamId);
        } else {
            legalRepresentativeOrganisation = createNewLegalRepresentative(organisationId,
                                                                           organisationDetails.getName(),
                                                               idamId, organisationDetails, caseEntity);
        }

        legalRepresentativeOrganisation.addParty(defendantPartyEntity);

        legalRepresentativeOrganisationRepository.save(legalRepresentativeOrganisation);
    }

    private LegalRepresentativeOrganisationEntity createNewLegalRepresentative(String id, String name, UUID idamId,
                                                                   OrganisationDetailsResponse organisationDetails,
                                                                               PcsCaseEntity caseEntity) {
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation =
            LegalRepresentativeOrganisationEntity.builder()
            .organisationId(id)
            .organisationName(name)
            .address(addressMapper.toAddressEntityAndNormalise(
                organisationDetailsService.getOrganisationAddress(organisationDetails)))
            .build();

        LegalRepresentativeEntity legalRepresentative = LegalRepresentativeEntity.builder()
            .idamId(idamId)
            .build();

        legalRepresentativeOrganisation.addLegalRepresentative(legalRepresentative);

        caseEntity.addLegalRepresentativeOrganisation(legalRepresentativeOrganisation);
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

    private void backfillLegalRepresentative(LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation,
                                             UUID idamId) {
        boolean isLegalRepresentativeNotPresent = legalRepresentativeOrganisation.getLegalRepresentativeList().stream()
            .noneMatch(legalRepresentative -> legalRepresentative.getIdamId().equals(
            idamId));

        if (isLegalRepresentativeNotPresent) {
            LegalRepresentativeEntity legalRepresentative = LegalRepresentativeEntity.builder()
                .idamId(idamId)
                .build();

            legalRepresentativeOrganisation.addLegalRepresentative(legalRepresentative);
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

            existingLegalRepresentativeOrganisation.getPartyLegalRepresentativeOrganisationList().stream()
                .filter(partyLegalRepresentativeOrganisation ->
                            partyLegalRepresentativeOrganisation.getParty().getId().equals(partyId))
                .forEach(this::invalidatePartyLegalRepresentativeOrganisation);

            legalRepresentativeOrganisationRepository.save(existingLegalRepresentativeOrganisation);
        }
    }

    private void invalidatePartyLegalRepresentativeOrganisation(PartyLegalRepresentativeOrganisationEntity
                                                                    partyLegalRepOrg) {
        partyLegalRepOrg.setActive(YesOrNo.NO);
        partyLegalRepOrg.setEndDate(Instant.now());
    }

}
