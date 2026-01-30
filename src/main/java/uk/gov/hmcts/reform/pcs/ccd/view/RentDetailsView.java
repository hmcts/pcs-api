package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;

@Component
public class RentDetailsView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        TenancyLicenceEntity tenancyLicence = pcsCaseEntity.getTenancyLicence();

        if (tenancyLicence != null) {
            RentDetails rentDetails = RentDetails.builder()
                .currentRent(tenancyLicence.getRentAmount())
                .frequency(tenancyLicence.getRentFrequency())
                .otherFrequency(tenancyLicence.getOtherRentFrequency())
                .dailyCharge(tenancyLicence.getRentPerDay())
                .build();

            pcsCase.setRentDetails(rentDetails);
        }
    }

}
