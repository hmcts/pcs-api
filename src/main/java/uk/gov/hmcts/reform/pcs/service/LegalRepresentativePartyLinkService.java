package uk.gov.hmcts.reform.pcs.service;

import static java.util.Objects.isNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
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
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.util.RevokeAccessHelper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalRepresentativePartyLinkService {

    private final PcsCaseService pcsCaseService;
    private final LegalRepresentativeOrganisationRepository legalRepOrgRepository;
    private final OrganisationDetailsService orgDetailsService;
    private final RevokeAccessHelper revokeAccessHelper;
    private final AddressMapper addressMapper;
    private final CaseRoleAssignmentService caseRoleAssignmentService;

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String partyId,
                                               OrganisationDetailsResponse organisationDetails) {
        String organisationId = organisationDetails.getOrganisationIdentifier();
        if (isAlreadyLinkedToParty(partyId, organisationId)) {
            throw new LegalRepresentativeAlreadyLinkedToPartyException(
                "Legal Representative or organisation already linked to Party [" + partyId + "]");
        }
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        checkConflictOfInterest(caseEntity, organisationDetails.getOrganisationIdentifier());

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

        unlinkExistingRepresentation(caseEntity, defendantPartyEntity);

        var legalRepOrg = legalRepOrgRepository.findByOrganisationIdAndCaseReference(organisationId, caseReference)
            .orElse(createNewLegalRepresentative(organisationId, organisationDetails, caseEntity));

        backfillOrganisationMetadata(legalRepOrg, organisationDetails);

        legalRepOrg.addParty(defendantPartyEntity);

        legalRepOrgRepository.save(legalRepOrg);
    }

    private boolean isAlreadyLinkedToParty(String partyId, String organisationId) {
        UUID targetPartyId = UUID.fromString(partyId);

        return legalRepOrgRepository
            .isRepresentativeOrganisationLinkedToPartyAndActive(organisationId, targetPartyId);
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

    private void backfillOrganisationMetadata(LegalRepresentativeOrganisationEntity legalRepOrg,
                                              OrganisationDetailsResponse orgDetails) {
        if (isNull(legalRepOrg.getOrganisationId())) {
            legalRepOrg.setOrganisationId(orgDetails.getOrganisationIdentifier());
        }
        if (isNull(legalRepOrg.getOrganisationName())) {
            legalRepOrg.setOrganisationName(orgDetails.getName());
        }
        if (isNull(legalRepOrg.getOrganisationProfileId())) {
            legalRepOrg.setOrganisationProfileId(orgDetails.getOrgProfileId());
        }
    }

    //TODO - check with Jonathan/Daniel why not just do caseEntity.getParties()
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
        var legalRepOrgEntity = legalRepOrgRepository
                .findByPartyLinkedToLegalRepresentativeOrganisationAndCaseAndActive(
                    defendantParty.getId(), caseEntity.getCaseReference());

        legalRepOrgEntity
            .ifPresent(legalRepOrg ->
                           revokeAccessHelper.revokeOrgAccessToRespondToClaim(caseEntity, legalRepOrg, defendantParty));

        revokeAccessHelper.revokeDefendantsAccessToRespondToClaim(caseEntity, defendantParty);
    }

    private LegalRepresentativeOrganisationEntity createNewLegalRepresentative(String id,
                                                                               OrganisationDetailsResponse orgDetails,
                                                                               PcsCaseEntity pcsCase) {

        var legalRepOrganisation = LegalRepresentativeOrganisationEntity.builder()
            .organisationId(id)
            .organisationName(orgDetails.getName())
            .organisationProfileId(orgDetails.getOrgProfileId())
            .build();

        var legalRepOrgContactDetails = LegalRepresentativeOrganisationContactDetailsEntity.builder()
                .pcsCase(pcsCase)
                .legalRepresentativeOrganisation(legalRepOrganisation)
                .address(addressMapper
                             .toAddressEntityAndNormalise(orgDetailsService.getOrganisationAddress(orgDetails)))
                .build();

        legalRepOrganisation.setLegalRepresentativeOrganisationContactDetails(legalRepOrgContactDetails);

        return legalRepOrganisation;
    }
}
