package uk.gov.hmcts.reform.pcs.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalRepresentativePartyLinkService {

    private final IdamService idamService;
    private final PcsCaseService pcsCaseService;
    private final OrganisationDetailsService organisationDetailsService;
    private final LegalRepresentativeRepository legalRepresentativeRepository;
    private final AddressMapper addressMapper;

    @Transactional
    public void linkLegalRepresentativeToParty(long caseReference, String authToken, String partyId) {
        var user = idamService.validateAuthToken(authToken).getUserDetails();
        String userUid = user.getUid();
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        PartyEntity defendantPartyEntity = getDefendantPartyEntity(caseEntity, partyId);

//        OrganisationDetailsResponse organisationDetails =
//        organisationDetailsService.getOrganisationDetails(user.getUid());
        // get contact details

        LegalRepresentativeEntity legalRepresentative = LegalRepresentativeEntity.builder()
            .organisationName(organisationDetailsService.getOrganisationName(userUid))
            .idamId(UUID.fromString(userUid))
            .firstName(user.getName())
            .lastName(user.getFamilyName())
            .email("e-mail")
            .phone("0121")
            .address(addressMapper.toAddressEntityAndNormalise(organisationDetailsService.getOrganisationAddress(userUid)))
            .build();

        legalRepresentative.addParty(defendantPartyEntity);

        legalRepresentativeRepository.save(legalRepresentative);
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

}
