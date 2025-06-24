package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.Address;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;

import java.util.stream.Collectors;

@Service
public class PCSCaseService {

    public PCSCase convertToPCSCase(PCS pcs) {
        if (pcs == null) {
            return null;
        }
        PCSCase.PCSCaseBuilder builder = PCSCase.builder()
            .ccdCaseReference(pcs.getCcdCaseReference());
        if (pcs.getAddress() != null) {
            builder.propertyAddress(convertAddress(pcs.getAddress()));
        }

        if (pcs.getGeneralApplications() != null) {
            builder.generalApplicationList(
                pcs.getGeneralApplications().stream()
                    .map(this::convertGenApplication)
                    .collect(Collectors.toList())
            );
        }
        return builder.build();
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
            .parentCaseReference(ga.getPcsCase().getCcdCaseReference())
            .applicationId(ga.getApplicationId())
            .adjustment(ga.getAdjustment()) // what about applicationID?
            .status(ga.getStatus())
            .build();
    }
}
