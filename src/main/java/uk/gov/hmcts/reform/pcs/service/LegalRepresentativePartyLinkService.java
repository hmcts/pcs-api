package uk.gov.hmcts.reform.pcs.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.LegalRepresentativeAlreadyLinkedToPartyException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalRepresentativePartyLinkService {

    private final PcsCaseService pcsCaseService;
    private final LegalRepresentativeRepository legalRepresentativeRepository;
    private final OrganisationDetailsService organisationDetailsService;
    private final AddressMapper addressMapper;

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String partyId, UserInfo user) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);
        OrganisationDetailsResponse organisationDetails = organisationDetailsService.getOrganisationDetails(user.getUid());
        String organisationId = organisationDetails.getOrganisationIdentifier();

        if (isAlreadyLinkedToParty(user, partyId, organisationId)) {
            throw new LegalRepresentativeAlreadyLinkedToPartyException(
                "Legal Representative or organisation already linked to Party [" + partyId + "]");
        }
        unlinkExistingRepresentation(UUID.fromString(partyId));

        Optional<LegalRepresentativeEntity> legalRepresentativeEntity = findExistingRepresentative(
            UUID.fromString(user.getUid()),
            organisationId
        );

        LegalRepresentativeEntity legalRepresentative;

        if (legalRepresentativeEntity.isPresent()) {
            legalRepresentative = legalRepresentativeEntity.get();
            backfillOrganisationMetadata(legalRepresentative, organisationDetails);
            legalRepresentative.addParty(defendantPartyEntity);
        } else {
            legalRepresentative = LegalRepresentativeEntity.builder()
                .organisationId(organisationId)
                .organisationName(organisationDetails.getName())
                .idamId(UUID.fromString(user.getUid()))
                .firstName(user.getName())
                .lastName(user.getFamilyName())
                .email(user.getSub())
                .address(addressMapper.toAddressEntityAndNormalise(
                    organisationDetailsService.getOrganisationAddress(organisationDetails)))
                .build();
        }

        legalRepresentative.addParty(defendantPartyEntity);

        legalRepresentativeRepository.saveAndFlush(legalRepresentative);
    }

    private boolean isAlreadyLinkedToParty(UserInfo user, String partyId, String organisationId) {
        UUID userId = UUID.fromString(user.getUid());
        UUID targetPartyId = UUID.fromString(partyId);

        if (isNotBlank(organisationId)) {
            return legalRepresentativeRepository.isRepresentativeOrganisationLinkedToPartyAndActive(
                organisationId,
                targetPartyId
            );
        }

        return legalRepresentativeRepository.isLegalRepresentativeLinkedToPartyAndActive(userId, targetPartyId);
    }

    private Optional<LegalRepresentativeEntity> findExistingRepresentative(UUID userId, String organisationId) {
        if (isNotBlank(organisationId)) {
            Optional<LegalRepresentativeEntity> byOrganisation =
                legalRepresentativeRepository.findByOrganisationId(organisationId);
            if (byOrganisation.isPresent()) {
                return byOrganisation;
            }
        }

        return legalRepresentativeRepository.findByIdamId(userId);
    }

    private void backfillOrganisationMetadata(LegalRepresentativeEntity legalRepresentative,
                                              OrganisationDetailsResponse organisationDetails) {
        if (legalRepresentative.getOrganisationId() == null) {
            legalRepresentative.setOrganisationId(organisationDetails.getOrganisationIdentifier());
        }
        if (legalRepresentative.getOrganisationName() == null) {
            legalRepresentative.setOrganisationName(organisationDetails.getName());
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
        Optional<LegalRepresentativeEntity> partyLinkedToLegalRepresentativeAndActive =
            legalRepresentativeRepository.isPartyLinkedToLegalRepresentativeAndActive(partyId);

        if (partyLinkedToLegalRepresentativeAndActive.isPresent()) {
            LegalRepresentativeEntity existingLegalRepresentative = partyLinkedToLegalRepresentativeAndActive.get();

            existingLegalRepresentative.getClaimPartyLegalRepresentativeList().stream()
                .filter(claimPartyLegalRepresentative ->
                            claimPartyLegalRepresentative.getParty().getId().equals(partyId))
                .forEach(this::invalidateLegalRepresentativeClaimParty);

            legalRepresentativeRepository.saveAndFlush(existingLegalRepresentative);
        }
    }

    private void invalidateLegalRepresentativeClaimParty(ClaimPartyLegalRepresentativeEntity claimParty) {
        claimParty.setActive(YesOrNo.NO);
        claimParty.setEndDate(Instant.now());
    }

}
