package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;

import java.math.BigDecimal;

@Component
public class RentDetailsView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        TenancyLicenceEntity tenancyLicence = pcsCaseEntity.getTenancyLicence();

        if (tenancyLicence != null) {
            BigDecimal rentAmount = tenancyLicence.getRentAmount();
            if (rentAmount != null) {
                pcsCase.setShowRentSectionPage(YesOrNo.YES);
            }

            RentDetails rentDetails = RentDetails.builder()
                .currentRent(rentAmount)
                .frequency(tenancyLicence.getRentFrequency())
                .otherFrequency(tenancyLicence.getOtherRentFrequency())
                .dailyCharge(tenancyLicence.getRentPerDay())
                .perDayCorrect(tenancyLicence.getCalculatedDailyRentCorrect())
                .build();

            pcsCase.setRentDetails(rentDetails);
        }
    }

}
