package uk.gov.hmcts.reform.pcs.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalRepresentativePartyLinkService {

    private final PcsCaseService pcsCaseService;
    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    private final OrganisationDetailsService organisationDetailsService;
    private final AddressMapper addressMapper;

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String partyId, UserInfo user) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

        if (legalRepresentativeOrganisationRepository.isLegalRepresentativeOrganisationLinkedToPartyAndActive(
            UUID.fromString(user.getUid()), UUID.fromString(partyId))) {
            throw new LegalRepresentativeAlreadyLinkedToPartyException(
                "Legal Representative [" + user.getUid() + "] already linked to Party [" + partyId + "]");
        }

        unlinkExistingRepresentation(UUID.fromString(partyId));

        Optional<LegalRepresentativeOrganisationEntity> legalRepresentativeEntity =
            legalRepresentativeOrganisationRepository.findByIdamId(UUID.fromString(user.getUid()));

        LegalRepresentativeOrganisationEntity legalRepresentative;

        if (legalRepresentativeEntity.isPresent()) {
            legalRepresentative = legalRepresentativeEntity.get();
            legalRepresentative.addParty(defendantPartyEntity);
        } else {
            OrganisationDetailsResponse organisationDetails = organisationDetailsService
                .getOrganisationDetails(user.getUid());

            legalRepresentative = LegalRepresentativeOrganisationEntity.builder()
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

        legalRepresentativeOrganisationRepository.saveAndFlush(legalRepresentative);
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

        legalRepresentativeOrganisationRepository.findLegalRepresentativeOrganisationForParty(partyId)
            .ifPresent((existingLegalRepresentative) -> {
                existingLegalRepresentative.getPartyLegalRepresentativeOrganisationList().stream()
                    .filter(claimPartyLegalRepresentative ->
                                claimPartyLegalRepresentative.getParty().getId().equals(partyId))
                    .forEach(this::invalidateLegalRepresentativeClaimParty);

                legalRepresentativeOrganisationRepository.saveAndFlush(existingLegalRepresentative);
            });
    }

    private void invalidateLegalRepresentativeClaimParty(PartyLegalRepresentativeOrganisationEntity claimParty) {
        claimParty.setActive(YesOrNo.NO);
        claimParty.setEndDate(Instant.now());
    }

}
