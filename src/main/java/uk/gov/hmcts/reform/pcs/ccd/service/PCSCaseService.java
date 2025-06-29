package uk.gov.hmcts.reform.pcs.ccd.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.Address;
import uk.gov.hmcts.reform.pcs.ccd.entity.GA;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;

import java.util.stream.Collectors;

@Service
public class PCSCaseService {

    private final ModelMapper modelMapper;
    private final PCSCaseRepository pcsCaseRepository;

    public PCSCaseService(ModelMapper modelMapper, PCSCaseRepository pcsCaseRepository) {
        this.modelMapper = modelMapper;
        this.pcsCaseRepository = pcsCaseRepository;
    }

    public PCSCase convertToPCSCase(PCS pcs) {
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

    public PCS convertToPCSEntity(PCSCase pcsCase) {
        return modelMapper.map(pcsCase, PCS.class);
    }

    private AddressUK convertAddress(Address address) {
        return AddressUK.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .postTown(address.getPostTown())
            .county(address.getCounty())
            .postCode(address.getPostCode())
            .country(address.getCountry())
            .build();
    }

    private GACase convertGenApplication(GA ga) {
        return GACase.builder()
            .caseReference(ga.getCaseReference())
            .adjustment(ga.getAdjustment())
            .additionalInformation(ga.getAdditionalInformation())
            .status(ga.getStatus())
            .build();
    }

    public PCS findPCSCase(Long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new IllegalStateException("PCS case not found"));
    }
}
