package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

@Service
@AllArgsConstructor
public class PcsCaseMergeService {

    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private final TenancyLicenceService tenancyLicenceService;

    public void mergeCaseData(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {

        if (pcsCase.getPropertyAddress() != null) {
            AddressEntity addressEntity = modelMapper.map(pcsCase.getPropertyAddress(), AddressEntity.class);
            pcsCaseEntity.setPropertyAddress(addressEntity);
        }

        if (pcsCase.getUserPcqId() != null) {
            setPcqIdForCurrentUser(pcsCase.getUserPcqId(), pcsCaseEntity);
        }

        if (pcsCase.getCaseManagementLocation() != null) {
            pcsCaseEntity.setCaseManagementLocation(pcsCase.getCaseManagementLocation());
        }

        if (pcsCase.getCaseLinks() != null) {
            pcsCaseEntity.mergeCaseLinks(pcsCase.getCaseLinks());
        }

        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.createTenancyLicenceEntity(pcsCase));
    }

    private void setPcqIdForCurrentUser(String pcqId, PcsCaseEntity pcsCaseEntity) {
        UserInfo userDetails = securityContextService.getCurrentUserDetails();
        UUID userId = UUID.fromString(userDetails.getUid());
        pcsCaseEntity.getParties().stream()
            .filter(party -> userId.equals(party.getIdamId()))
            .findFirst()
            .orElseGet(() -> {
                PartyEntity party = createPartyForUser(userId, userDetails);
                pcsCaseEntity.addParty(party);
                return party;
            })
            .setPcqId(pcqId);
    }

    private static PartyEntity createPartyForUser(UUID userId, UserInfo userDetails) {
        PartyEntity party = new PartyEntity();
        party.setIdamId(userId);
        party.setFirstName(userDetails.getGivenName());
        party.setLastName(userDetails.getFamilyName());
        return party;
    }

}
