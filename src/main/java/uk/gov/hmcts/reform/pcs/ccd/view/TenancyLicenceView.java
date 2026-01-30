package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;

@Component
public class TenancyLicenceView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        TenancyLicenceEntity tenancyLicence = pcsCaseEntity.getTenancyLicence();

        if (tenancyLicence != null) {
            TenancyLicenceDetails tenancyLicenceDetails = TenancyLicenceDetails.builder()
                .typeOfTenancyLicence(tenancyLicence.getType())
                .tenancyLicenceDate(tenancyLicence.getStartDate())
                .detailsOfOtherTypeOfTenancyLicence(tenancyLicence.getOtherTypeDetails())
                .build();

            pcsCase.setTenancyLicenceDetails(tenancyLicenceDetails);
        }
    }

}
