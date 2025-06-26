package uk.gov.hmcts.reform.pcs.ccd.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.Address;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;
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
            .ccdCaseReference(pcs.getCcdCaseReference());
        if (pcs.getPropertyAddress() != null) {
            builder.propertyAddress(convertAddress(pcs.getPropertyAddress()));
        }
        if (pcs.getGeneralApplications() != null) {
            builder.generalApplications(
                pcs.getGeneralApplications().stream()
                    .map(ga -> {
                        GeneralApplication dto = convertGenApplication(ga);
                        return ListValue.<GeneralApplication>builder()
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

    private GeneralApplication convertGenApplication(GenApplication ga) {
        return GeneralApplication.builder()
            .applicationId(ga.getApplicationId())
            .adjustment(ga.getAdjustment())
            .additionalInformation(ga.getAdditionalInformation())
            .status(ga.getStatus())
            .build();
    }

    public PCS findParentCase(Long caseReference) {
        return pcsCaseRepository.findByCcdCaseReference(caseReference)
            .orElseThrow(() -> new IllegalStateException("Parent case not found"));
    }
}
