package uk.gov.hmcts.reform.pcs.ccd.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimantInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.GACaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.stream.Collectors;

@Service

public class PCSCaseService {


    private final ModelMapper modelMapper;
    private final PCSCaseRepository pcsCaseRepository;

    public PCSCaseService(ModelMapper modelMapper, PCSCaseRepository pcsCaseRepository) {
        this.modelMapper = modelMapper;
        this.pcsCaseRepository = pcsCaseRepository;
    }

    public PCSCase convertToPCSCase(PcsCaseEntity pcs) {
        PCSCase.PCSCaseBuilder builder = PCSCase.builder()
            .caseReference(pcs.getCaseReference());
        if (pcs.getPropertyAddress() != null) {
            builder.propertyAddress(convertAddress(pcs.getPropertyAddress()));
        }
        if (pcs.getClaimantInfo() != null) {
            builder.applicantForename(pcs.getClaimantInfo().getForename());
            builder.applicantSurname(pcs.getClaimantInfo().getSurname());
        }
        if (pcs.getGeneralApplications() != null) {
            builder.generalApplications(
                pcs.getGeneralApplications().stream()
                    .map(ga -> {
                        GACase dto = convertGenApplication(ga);
                        return ListValue.<GACase>builder()
                            .id(ga.getId().toString())
                            .value(dto)
                            .build();
                    })
                    .collect(Collectors.toList())
            );
        }
        return builder.build();
    }

    public void createCase(long caseReference, PCSCase pcsCase) {
        AddressUK applicantAddress = pcsCase.getPropertyAddress();

        AddressEntity addressEntity = applicantAddress != null
            ? modelMapper.map(applicantAddress, AddressEntity.class) : null;
        ClaimantInfo claimantInfo = createClaimantInfoEntity(pcsCase);
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setClaimantInfo(claimantInfo);
        ;
        pcsCaseEntity.setPropertyAddress(addressEntity);

        pcsCaseRepository.save(pcsCaseEntity);
    }

    public void patchCase(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        ClaimantInfo claimantInfo = new ClaimantInfo();


        if (pcsCase.getApplicantForename() != null) {
            claimantInfo.setForename(pcsCase.getApplicantForename());
        }

        if (pcsCase.getApplicantSurname() != null) {
            claimantInfo.setForename(pcsCase.getApplicantSurname());
            pcsCaseEntity.setClaimantInfo(claimantInfo);
        }

        if (pcsCase.getPropertyAddress() != null) {
            AddressEntity addressEntity = modelMapper.map(pcsCase.getPropertyAddress(), AddressEntity.class);
            pcsCaseEntity.setPropertyAddress(addressEntity);
        }

        pcsCaseRepository.save(pcsCaseEntity);
    }

    private AddressUK convertAddress(AddressEntity address) {
        return AddressUK.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .postTown(address.getPostTown())
            .county(address.getCounty())
            .postCode(address.getPostCode())
            .country(address.getCountry())
            .build();
    }

    private GACase convertGenApplication(GACaseEntity ga) {
        return GACase.builder()
            .caseReference(ga.getCaseReference())
            .gaType(ga.getGaType())
            .adjustment(ga.getAdjustment())
            .additionalInformation(ga.getAdditionalInformation())
            .status(ga.getStatus())
            .build();
    }

    public PcsCaseEntity findPCSCase(Long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new IllegalStateException("PCS case not found"));
    }

    public PcsCaseEntity savePCSCase(PcsCaseEntity pcsCase) {
        return pcsCaseRepository.save(pcsCase);
    }

    public ClaimantInfo createClaimantInfoEntity(PCSCase pcsCase) {
        ClaimantInfo claimantInfo = ClaimantInfo.builder().forename(pcsCase.getApplicantForename())
            .surname(pcsCase.getApplicantSurname()).build();
        return claimantInfo;
    }
}
