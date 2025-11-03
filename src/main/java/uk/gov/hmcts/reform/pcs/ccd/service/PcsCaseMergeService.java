package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.mapper.DefendantMapper;
import uk.gov.hmcts.reform.pcs.ccd.model.PossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.model.SecureOrFlexibleReasonsForGrounds;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

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
    private final DefendantMapper defendantMapper;

    public void mergeCaseData(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {

        if (pcsCase.getPropertyAddress() != null) {
            AddressEntity addressEntity = modelMapper.map(pcsCase.getPropertyAddress(), AddressEntity.class);
            pcsCaseEntity.setPropertyAddress(addressEntity);
        }

        if (pcsCase.getUserPcqId() != null) {
            UUID pcqId = UUID.fromString(pcsCase.getUserPcqId());
            setPcqIdForCurrentUser(pcqId, pcsCaseEntity);
        }

        if (pcsCase.getCaseManagementLocation() != null) {
            pcsCaseEntity.setCaseManagementLocation(pcsCase.getCaseManagementLocation());
        }

        if (pcsCase.getPreActionProtocolCompleted() != null) {
            pcsCaseEntity.setPreActionProtocolCompleted(pcsCase.getPreActionProtocolCompleted().toBoolean());
        }

        // Merge claimant type if available
        if (pcsCase.getClaimantType() != null && pcsCase.getClaimantType().getValueCode() != null) {
            ClaimantType claimantType = ClaimantType.valueOf(pcsCase.getClaimantType().getValueCode());
            pcsCaseEntity.setClaimantType(claimantType);
        }

        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.buildTenancyLicence(pcsCase));
        pcsCaseEntity.setPossessionGrounds(buildPossessionGrounds(pcsCase));
        pcsCaseEntity.setDefendants(defendantMapper.mapFromDefendantDetails(pcsCase.getDefendants()));
    }

    private void setPcqIdForCurrentUser(UUID pcqId, PcsCaseEntity pcsCaseEntity) {
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
        party.setForename(userDetails.getGivenName());
        party.setSurname(userDetails.getFamilyName());
        party.setActive(true);
        return party;
    }

    private PossessionGrounds buildPossessionGrounds(PCSCase pcsCase) {
        SecureOrFlexibleReasonsForGrounds reasons = Optional.ofNullable(pcsCase.getSecureOrFlexibleGroundsReasons())
            .map(grounds -> modelMapper.map(grounds, SecureOrFlexibleReasonsForGrounds.class))
            .orElse(SecureOrFlexibleReasonsForGrounds.builder().build());

        // Default to Secure/Flexible mappings (labels)
        Set<String> mandatoryGrounds = mapToLabels(pcsCase.getSecureOrFlexibleMandatoryGrounds());
        Set<String> discretionaryGrounds = mapToLabels(pcsCase.getSecureOrFlexibleDiscretionaryGrounds());

        // If this is an Assured Tenancy, mirror the Assured selections into possessionGrounds
        if (pcsCase.getTypeOfTenancyLicence() == TenancyLicenceType.ASSURED_TENANCY) {
            if (pcsCase.getGroundsForPossession() == YesOrNo.YES) {
                // Rent arrears path (canonical sets already built during pages)
                mandatoryGrounds = mapToEnumNames(pcsCase.getRentArrearsMandatoryGrounds());
                discretionaryGrounds = mapToEnumNames(pcsCase.getRentArrearsDiscretionaryGrounds());
            } else if (pcsCase.getGroundsForPossession() == YesOrNo.NO) {
                // No-rent-arrears path
                mandatoryGrounds = mapToEnumNames(pcsCase.getNoRentArrearsMandatoryGroundsOptions());
                discretionaryGrounds = mapToEnumNames(pcsCase.getNoRentArrearsDiscretionaryGroundsOptions());
            }
        }

        return PossessionGrounds.builder()
            .mandatoryGrounds(mandatoryGrounds)
            .discretionaryGrounds(discretionaryGrounds)
            .discretionaryGroundsAlternativeAccommodation(mapToLabels(
                pcsCase.getSecureOrFlexibleDiscretionaryGroundsAlt())
            )
            .mandatoryGroundsAlternativeAccommodation(mapToLabels(pcsCase.getSecureOrFlexibleMandatoryGroundsAlt()))
            .walesDiscretionaryGrounds(mapToLabels(pcsCase.getDiscretionaryGroundsWales()))
            .walesMandatoryGrounds(mapToLabels(pcsCase.getMandatoryGroundsWales()))
            .walesEstateManagementGrounds(mapToLabels(pcsCase.getEstateManagementGroundsWales()))
            .walesSecureContractDiscretionaryGrounds(mapToLabels(pcsCase.getSecureContractDiscretionaryGroundsWales()))
            .walesSecureContractMandatoryGrounds(mapToLabels(pcsCase.getSecureContractMandatoryGroundsWales()))
            .walesSecureContractEstateManagementGrounds(
                mapToLabels(pcsCase.getSecureContractEstateManagementGroundsWales()))
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

    private <E extends Enum<E>> Set<String> mapToEnumNames(Set<E> items) {
        return Optional.ofNullable(items)
            .orElse(Collections.emptySet())
            .stream()
            .map(Enum::name)
            .collect(Collectors.toSet());
    }

}
