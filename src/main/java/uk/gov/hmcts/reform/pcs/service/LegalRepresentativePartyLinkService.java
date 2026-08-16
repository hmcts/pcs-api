package uk.gov.hmcts.reform.pcs.service;


import static java.util.UUID.fromString;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationRepository;
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
    private final LegalRepresentativeOrganisationRepository legalRepOrgRepository;
    private final ClaimPartyLegalRepresentativeOrganisationRepository claimPartyLegalRepOrgRepository;
    private final OrganisationDetailsService organisationDetailsService;
    private final AddressMapper addressMapper;

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String partyId,
                                               OrganisationDetailsResponse organisationDetails) {
        String organisationId = organisationDetails.getOrganisationIdentifier();
        if (isAlreadyLinkedToParty(partyId, organisationId)) {
            throw new LegalRepresentativeAlreadyLinkedToPartyException(
                "Legal Representative or organisation already linked to Party [" + partyId + "]");
        }

        unlinkExistingRepresentation(fromString(partyId));

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation;

        var existingOrganisation =
            legalRepOrgRepository.findByOrganisationIdAndCaseReference(organisationId, caseReference);

        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        if (existingOrganisation.isPresent()) {
            legalRepresentativeOrganisation = existingOrganisation.get();
            backfillOrganisationMetadata(legalRepresentativeOrganisation, organisationDetails);
        } else {
            legalRepresentativeOrganisation = createNewLegalRepresentative(
                organisationId,
                organisationDetails,
                caseEntity);
        }
        legalRepresentativeOrganisation.addParty(getDefendantPartyEntity(caseEntity, partyId));
        legalRepOrgRepository.save(legalRepresentativeOrganisation);
    }


    private LegalRepresentativeOrganisationEntity createNewLegalRepresentative(String id,
                                                                               OrganisationDetailsResponse orgDetails,
                                                                               PcsCaseEntity pcsCase) {

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation = LegalRepresentativeOrganisationEntity
            .builder()
            .organisationId(id)
            .organisationName(orgDetails.getName())
            .organisationProfileId(orgDetails.getOrgProfileId())
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
        UUID targetPartyId = fromString(partyId);

        return legalRepOrgRepository
            .isRepresentativeOrganisationLinkedToPartyAndActive(organisationId, targetPartyId);
    }

    private Optional<LegalRepresentativeOrganisationEntity> retrieveOrganisationIfExists(
        String organisationId, long caseReference) {
        return legalRepOrgRepository.findByOrganisationIdAndCaseReference(organisationId, caseReference);
    }

    private void backfillOrganisationMetadata(LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation,
                                              OrganisationDetailsResponse organisationDetails) {
        if (legalRepresentativeOrganisation.getOrganisationId() == null) {
            legalRepresentativeOrganisation.setOrganisationId(organisationDetails.getOrganisationIdentifier());
        }
        if (legalRepresentativeOrganisation.getOrganisationName() == null) {
            legalRepresentativeOrganisation.setOrganisationName(organisationDetails.getName());
        }
        if (legalRepresentativeOrganisation.getOrganisationProfileId() == null) {
            legalRepresentativeOrganisation.setOrganisationProfileId(organisationDetails.getOrgProfileId());
        }
    }

    private PartyEntity getDefendantPartyEntity(PcsCaseEntity caseEntity, String partyId) {
        return caseEntity.getClaims().getFirst()
            .getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .filter(partyEntity -> partyEntity.getId().equals(fromString(partyId)))
            .findFirst()
            .orElseThrow(() -> {
                log.error("Unable to find Party [{}]", partyId);
                return new PartyNotFoundException("Unable to find Party with Id [" + partyId + "]");
            });
    }

    private void unlinkExistingRepresentation(UUID partyId) {
        claimPartyLegalRepOrgRepository
            .findByPartyIdAndActive(partyId, YesOrNo.YES)
            .ifPresent(partyLegalRepOrg -> {
                invalidatePartyLegalRepOrg(partyLegalRepOrg);
                claimPartyLegalRepOrgRepository.save(partyLegalRepOrg);
            });
    }

    private void invalidatePartyLegalRepOrg(ClaimPartyLegalRepresentativeOrganisationEntity partyLegalRepOrg) {
        partyLegalRepOrg.setActive(YesOrNo.NO);
        partyLegalRepOrg.setEndDate(Instant.now());
    }

}
