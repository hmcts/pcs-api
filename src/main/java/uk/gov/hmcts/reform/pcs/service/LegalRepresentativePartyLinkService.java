package uk.gov.hmcts.reform.pcs.service;

import static java.util.Objects.isNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
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
    private final Clock utcClock;

    public LegalRepresentativePartyLinkService(PcsCaseService pcsCaseService,
                                               OrganisationRepository
                                                   organisationRepository,
                                               ClaimPartyContactDetailsRepository
                                                   legalRepOrganisationContactDetailsRepository,
                                               OrganisationDetailsService organisationDetailsService,
                                               AddressMapper addressMapper,
                                               RevokeAccessHelper revokeAccessHelper,
                                               @Qualifier("utcClock") Clock utcClock) {
        this.pcsCaseService = pcsCaseService;
        this.organisationRepository = organisationRepository;
        this.legalRepOrganisationContactDetailsRepository = legalRepOrganisationContactDetailsRepository;
        this.organisationDetailsService = organisationDetailsService;
        this.addressMapper = addressMapper;
        this.revokeAccessHelper = revokeAccessHelper;
        this.utcClock = utcClock;
    }

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String partyId,
                                               OrganisationDetailsResponse orgDetails) {
        String orgId = orgDetails.getOrganisationIdentifier();
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

        unlinkExistingRepresentation(caseEntity, defendantPartyEntity);

        Optional<OrganisationEntity> legalRepOrgEntity = organisationRepository.findByOrganisationId(orgId);

        OrganisationEntity legalRepOrg;

        if (legalRepOrgEntity.isPresent()) {
            legalRepOrg = legalRepOrgEntity.get();
            backfillOrganisationMetadata(legalRepOrg, orgDetails);

            Optional<ClaimPartyContactDetailsEntity> existingContactDetails =
                legalRepOrganisationContactDetailsRepository
                    .findByOrganisationIdAndCaseReference(orgId, caseReference);

            if (existingContactDetails.isEmpty()) {
                ClaimPartyContactDetailsEntity legalRepOrgContactDetails =
                    buildLegalRepresentativeOrganisationContactDetails(caseEntity, legalRepOrg, orgDetails);

                legalRepOrg.addClaimPartyContactDetails(legalRepOrgContactDetails);
            }
        } else {
            legalRepOrg = createNewLegalRepresentative(orgId, orgDetails, caseEntity);
        }
        legalRepOrg.addParty(defendantPartyEntity);
        organisationRepository.save(legalRepOrg);
    }

    private OrganisationEntity createNewLegalRepresentative(String id,
                                                            OrganisationDetailsResponse orgDetails,
                                                            PcsCaseEntity pcsCase) {

        OrganisationEntity legalRepresentativeOrganisation = OrganisationEntity
            .builder()
            .organisationId(id)
            .organisationName(orgDetails.getName())
            .organisationProfileId(orgDetails.getOrgProfileId())
            .createdDate(LocalDateTime.now(utcClock))
            .build();

        ClaimPartyContactDetailsEntity legalRepresentativeOrganisationContactDetails =
            buildLegalRepresentativeOrganisationContactDetails(pcsCase, legalRepresentativeOrganisation, orgDetails);

        legalRepresentativeOrganisation
            .addClaimPartyContactDetails(legalRepresentativeOrganisationContactDetails);

        return legalRepresentativeOrganisation;
    }

    private void backfillOrganisationMetadata(OrganisationEntity legalRepOrg,
                                              OrganisationDetailsResponse orgDetails) {
        if (legalRepOrg.getOrganisationId() == null) {
            legalRepOrg.setOrganisationId(orgDetails.getOrganisationIdentifier());
        }
        if (legalRepOrg.getOrganisationName() == null) {
            legalRepOrg.setOrganisationName(orgDetails.getName());
        }
        if (isNull(legalRepOrg.getOrganisationProfileId())) {
            legalRepOrg.setOrganisationProfileId(orgDetails.getOrgProfileId());
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
        Optional<OrganisationEntity> partyLinkedToLegalRepresentativeOrganisationAndActive =
            organisationRepository
                .findByPartyLinkedToOrganisationAndCaseAndActive(
                    defendantParty.getId(), caseEntity.getCaseReference());

        partyLinkedToLegalRepresentativeOrganisationAndActive
            .ifPresent(legalRepresentativeOrganisation -> revokeAccessHelper.revokeOrganisationAccessToRespondToClaim(
                caseEntity,
                partyLinkedToLegalRepresentativeOrganisationAndActive.get(),
                defendantParty
            ));

        revokeAccessHelper.revokeDefendantsAccessToRespondToClaim(caseEntity, defendantParty);
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
