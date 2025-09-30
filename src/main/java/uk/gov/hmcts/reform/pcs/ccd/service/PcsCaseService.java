package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.model.PossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.model.SecureOrFlexibleReasonsForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PcsCaseService {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private final TenancyLicenceService tenancyLicenceService;

    public void createCase(long caseReference, AddressUK propertyAddress, LegislativeCountry legislativeCountry) {

        Objects.requireNonNull(propertyAddress, "Property address must be provided to create a case");
        Objects.requireNonNull(legislativeCountry, "Legislative country must be provided to create a case");

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setPropertyAddress(modelMapper.map(propertyAddress, AddressEntity.class));
        pcsCaseEntity.setLegislativeCountry(legislativeCountry);

        pcsCaseRepository.save(pcsCaseEntity);
    }

    public void createCase(long caseReference, PCSCase pcsCase) {
        AddressUK applicantAddress = pcsCase.getPropertyAddress();

        AddressEntity addressEntity = applicantAddress != null
            ? modelMapper.map(applicantAddress, AddressEntity.class) : null;

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setPropertyAddress(addressEntity);
        pcsCaseEntity.setPaymentStatus(pcsCase.getPaymentStatus());
        pcsCaseEntity.setPreActionProtocolCompleted(
                pcsCase.getPreActionProtocolCompleted() != null
                        ? pcsCase.getPreActionProtocolCompleted().toBoolean()
                        : null);
        pcsCaseEntity.setDefendants(mapFromDefendantDetails(pcsCase.getDefendants()));
        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.buildTenancyLicence(pcsCase));

        pcsCaseRepository.save(pcsCaseEntity);
    }

    public PcsCaseEntity patchCase(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        if (pcsCase.getPropertyAddress() != null) {
            AddressEntity addressEntity = modelMapper.map(pcsCase.getPropertyAddress(), AddressEntity.class);
            pcsCaseEntity.setPropertyAddress(addressEntity);
        }

        if (pcsCase.getUserPcqId() != null) {
            UUID pcqId = UUID.fromString(pcsCase.getUserPcqId());
            setPcqIdForCurrentUser(pcqId, pcsCaseEntity);
        }

        if (pcsCase.getPaymentStatus() != null) {
            pcsCaseEntity.setPaymentStatus(pcsCase.getPaymentStatus());
        }

        if (pcsCase.getCaseManagementLocation() != null) {
            pcsCaseEntity.setCaseManagementLocation(pcsCase.getCaseManagementLocation());
        }

        if (pcsCase.getPreActionProtocolCompleted() != null) {
            pcsCaseEntity.setPreActionProtocolCompleted(pcsCase.getPreActionProtocolCompleted().toBoolean());
        }

        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.buildTenancyLicence(pcsCase));
        pcsCaseEntity.setPossessionGrounds(buildPossessionGrounds(pcsCase));

        pcsCaseRepository.save(pcsCaseEntity);

        return pcsCaseEntity;
    }

    public List<Defendant> mapFromDefendantDetails(List<ListValue<DefendantDetails>> defendants) {
        if (defendants == null) {
            return Collections.emptyList();
        }
        List<Defendant> result = new ArrayList<>();
        for (ListValue<DefendantDetails> item : defendants) {
            DefendantDetails details = item.getValue();
            if (details != null) {
                Defendant defendant = modelMapper.map(details, Defendant.class);
                defendant.setId(item.getId());
                if (details.getAddressSameAsPossession() == null) {
                    defendant.setAddressSameAsPossession(false);
                }
                result.add(defendant);
            }
        }
        return result;
    }

    public List<ListValue<DefendantDetails>> mapToDefendantDetails(List<Defendant> defendants) {
        if (defendants == null) {
            return Collections.emptyList();
        }
        List<ListValue<DefendantDetails>> result = new ArrayList<>();
        for (Defendant defendant : defendants) {
            if (defendant != null) {
                DefendantDetails details = modelMapper.map(defendant, DefendantDetails.class);
                result.add(new ListValue<>(defendant.getId(), details));
            }
        }
        return result;
    }

    public void clearHiddenDefendantDetailsFields(List<ListValue<DefendantDetails>> defendantsList) {
        if (defendantsList == null) {
            return;
        }

        for (ListValue<DefendantDetails> listValue : defendantsList) {
            DefendantDetails defendant = listValue.getValue();
            if (defendant != null) {
                if (VerticalYesNo.NO == defendant.getNameKnown()) {
                    defendant.setFirstName(null);
                    defendant.setLastName(null);
                }
                if (VerticalYesNo.NO == defendant.getAddressKnown()) {
                    defendant.setCorrespondenceAddress(null);
                    defendant.setAddressSameAsPossession(null);
                }
                if (VerticalYesNo.NO == defendant.getEmailKnown()) {
                    defendant.setEmail(null);
                }
            }
        }
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
            .map(grounds -> modelMapper.map(grounds,
                                            SecureOrFlexibleReasonsForGrounds.class))
            .orElse(SecureOrFlexibleReasonsForGrounds.builder().build());

        return PossessionGrounds.builder()
            .discretionaryGrounds(mapToLabels(pcsCase.getSecureOrFlexibleDiscretionaryGrounds()))
            .mandatoryGrounds(mapToLabels(pcsCase.getSecureOrFlexibleMandatoryGrounds()))
            .discretionaryGroundsAlternativeAccommodation(mapToLabels(
                pcsCase.getSecureOrFlexibleDiscretionaryGroundsAlt())
            )
            .mandatoryGroundsAlternativeAccommodation(mapToLabels(pcsCase.getSecureOrFlexibleMandatoryGroundsAlt()))
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
