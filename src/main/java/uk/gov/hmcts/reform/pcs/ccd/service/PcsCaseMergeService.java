package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexiblePossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.PossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.model.SecureOrFlexibleReasonsForGrounds;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.createTenancyLicenceEntity(pcsCase));
        pcsCaseEntity.setPossessionGrounds(buildPossessionGrounds(pcsCase));
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

    private PossessionGrounds buildPossessionGrounds(PCSCase pcsCase) {

        SecureContractGroundsForPossessionWales secureContractGroundsWales =
            Optional.ofNullable(pcsCase.getSecureContractGroundsForPossessionWales())
                .orElse(SecureContractGroundsForPossessionWales.builder().build());

        SecureOrFlexibleReasonsForGrounds reasons = Optional.ofNullable(pcsCase.getSecureOrFlexibleGroundsReasons())
            .map(grounds -> modelMapper
                .map(grounds, SecureOrFlexibleReasonsForGrounds.class))
            .orElse(SecureOrFlexibleReasonsForGrounds.builder().build());

        SecureOrFlexiblePossessionGrounds secureOrFlexiblePossessionGrounds = Optional.ofNullable(
            pcsCase.getSecureOrFlexiblePossessionGrounds()).orElse(SecureOrFlexiblePossessionGrounds.builder().build());

        GroundsForPossessionWales groundsForPossessionWales =
            Optional.ofNullable(pcsCase.getGroundsForPossessionWales())
                .orElse(GroundsForPossessionWales.builder().build());

        return PossessionGrounds.builder()
            .discretionaryGrounds(
                mapToLabels(secureOrFlexiblePossessionGrounds.getSecureOrFlexibleDiscretionaryGrounds()))
            .mandatoryGrounds(mapToLabels(secureOrFlexiblePossessionGrounds.getSecureOrFlexibleMandatoryGrounds()))
            .discretionaryGroundsAlternativeAccommodation(
                mapToLabels(secureOrFlexiblePossessionGrounds.getSecureOrFlexibleDiscretionaryGroundsAlt()))
            .mandatoryGroundsAlternativeAccommodation(
                mapToLabels(secureOrFlexiblePossessionGrounds.getSecureOrFlexibleMandatoryGroundsAlt()))
            .walesDiscretionaryGrounds(mapToLabels(groundsForPossessionWales.getDiscretionaryGroundsWales()))
            .walesMandatoryGrounds(mapToLabels(groundsForPossessionWales.getMandatoryGroundsWales()))
            .walesEstateManagementGrounds(mapToLabels(groundsForPossessionWales.getEstateManagementGroundsWales()))
            .walesSecureContractDiscretionaryGrounds(mapToLabels(secureContractGroundsWales
                                                                     .getDiscretionaryGroundsWales()))
            .walesSecureContractMandatoryGrounds(mapToLabels(secureContractGroundsWales
                                                                 .getMandatoryGroundsWales()))
            .walesSecureContractEstateManagementGrounds(mapToLabels(secureContractGroundsWales
                                .getEstateManagementGroundsWales()))
            .secureOrFlexibleReasonsForGrounds(reasons)
            .build();
    }

    private <T extends HasLabel> Set<String> mapToLabels(Set<T> items) {
        return Optional.ofNullable(items)
            .orElse(Collections.emptySet())
            .stream()
            .map(HasLabel::getLabel)
            .collect(Collectors.toSet());
    }

}
