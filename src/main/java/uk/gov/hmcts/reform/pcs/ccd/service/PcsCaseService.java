package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PcsCaseService {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;

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

        pcsCaseEntity.setTenancyLicence(buildTenancyLicence(pcsCase));

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

        pcsCaseEntity.setTenancyLicence(buildTenancyLicence(pcsCase));

        pcsCaseRepository.save(pcsCaseEntity);

        return pcsCaseEntity;
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

    //Temporary method to create tenancy_licence JSON and related fields
    // Data in this JSON will likely be moved to a dedicated entity in the future
    private TenancyLicence buildTenancyLicence(PCSCase pcsCase) {
        return TenancyLicence.builder()
                .noticeServed(toBooleanOrNull(pcsCase.getNoticeServed()))
                .build();
    }

    private static Boolean toBooleanOrNull(YesOrNo yesOrNo) {
        return yesOrNo != null ? yesOrNo.toBoolean() : null;
    }
}
