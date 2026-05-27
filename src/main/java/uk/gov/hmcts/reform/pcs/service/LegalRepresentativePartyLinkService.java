package uk.gov.hmcts.reform.pcs.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
        if (isAlreadyLinkedToParty(user, partyId, organisationId)) {
            throw new LegalRepresentativeAlreadyLinkedToPartyException(
                "Legal Representative or organisation already linked to Party [" + partyId + "]");
        }
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

        unlinkExistingRepresentation(UUID.fromString(partyId));

        UUID idamId = UUID.fromString(user.getUid());

        Optional<LegalRepresentativeOrganisationEntity> legalRepresentativeEntity = findExistingRepresentative(idamId,
                                                                                                   organisationId,
                                                                                                   caseReference
        );

        LegalRepresentativeOrganisationEntity legalRepresentative;

        if (legalRepresentativeEntity.isPresent()) {

            legalRepresentative = legalRepresentativeEntity.get();
            boolean legalRepresentativeLinkedToCase = isLegalRepresentativeLinkedToTheCase(caseReference,
                                                                                         legalRepresentative);

            if (legalRepresentativeLinkedToCase) {
                backfillOrganisationMetadata(legalRepresentative, organisationDetails);
                legalRepresentative.addParty(defendantPartyEntity);
            } else {
                legalRepresentative = createNewLegalRepresentative(organisationId, organisationDetails.getName(),
                                                                   idamId, organisationDetails);
            }
        } else {
            legalRepresentative = createNewLegalRepresentative(organisationId, organisationDetails.getName(),
                                                               idamId, organisationDetails);
        }

        legalRepresentative.addParty(defendantPartyEntity);

        legalRepresentativeOrganisationRepository.save(legalRepresentative);
    }

    private boolean isLegalRepresentativeLinkedToTheCase(long caseReference,
                                                         LegalRepresentativeOrganisationEntity legalRepresentative) {

        return legalRepresentative.getPartyLegalRepresentativeOrganisationList().stream()
            .anyMatch(claimPartyLegalRepresentative ->
                          claimPartyLegalRepresentative.getParty()
                              .getPcsCase().getCaseReference().equals(caseReference));
    }

    private LegalRepresentativeOrganisationEntity createNewLegalRepresentative(String id, String name, UUID idamId,
                                                                   OrganisationDetailsResponse organisationDetails) {
        return LegalRepresentativeOrganisationEntity.builder()
            .organisationId(id)
            .organisationName(name)
            .address(addressMapper.toAddressEntityAndNormalise(
                organisationDetailsService.getOrganisationAddress(organisationDetails)))
            .build();
    }

    private boolean isAlreadyLinkedToParty(UserInfo user, String partyId, String organisationId) {
        UUID userId = UUID.fromString(user.getUid());
        UUID targetPartyId = UUID.fromString(partyId);

        if (isNotBlank(organisationId)) {
            return legalRepresentativeOrganisationRepository.isRepresentativeOrganisationLinkedToPartyAndActive(
                organisationId,
                targetPartyId
            );
        }

        return legalRepresentativeOrganisationRepository.isLegalRepresentativeLinkedToPartyAndActive(userId, targetPartyId);
    }

    private Optional<LegalRepresentativeOrganisationEntity> findExistingRepresentative(UUID userId, String organisationId,
                                                                           long caseReference) {
        return Optional.ofNullable(organisationId)
            .filter(StringUtils::isNotBlank)
            .flatMap(id -> legalRepresentativeOrganisationRepository.findByOrganisationId(id, caseReference))
            .or(() -> legalRepresentativeOrganisationRepository.findByIdamId(userId, caseReference));
    }

    private void backfillOrganisationMetadata(LegalRepresentativeOrganisationEntity legalRepresentative,
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
        Optional<LegalRepresentativeOrganisationEntity> partyLinkedToLegalRepresentativeAndActive =
            legalRepresentativeOrganisationRepository.findByPartyLinkedToLegalRepresentativeAndActive(partyId);

        if (partyLinkedToLegalRepresentativeAndActive.isPresent()) {
            LegalRepresentativeOrganisationEntity existingLegalRepresentative = partyLinkedToLegalRepresentativeAndActive.get();

            existingLegalRepresentative.getPartyLegalRepresentativeOrganisationList().stream()
                .filter(claimPartyLegalRepresentative ->
                            claimPartyLegalRepresentative.getParty().getId().equals(partyId))
                .forEach(this::invalidateLegalRepresentativeClaimParty);

            legalRepresentativeOrganisationRepository.save(existingLegalRepresentative);
        }
    }

    private void invalidateLegalRepresentativeClaimParty(PartyLegalRepresentativeOrganisationEntity claimParty) {
        claimParty.setActive(YesOrNo.NO);
        claimParty.setEndDate(Instant.now());
    }

}
