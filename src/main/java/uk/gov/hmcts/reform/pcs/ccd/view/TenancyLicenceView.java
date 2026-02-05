package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Component
public class TenancyLicenceView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        TenancyLicenceEntity tenancyLicence = pcsCaseEntity.getTenancyLicence();

        if (tenancyLicence == null) {
            return;
        }

        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            setOccupationLicenceFields(pcsCase, tenancyLicence);
        } else {
            setTenancyLicenceFields(pcsCase, tenancyLicence);
        }
    }

    private static void setTenancyLicenceFields(PCSCase pcsCase, TenancyLicenceEntity tenancyLicence) {
        CombinedLicenceType combinedLicenceType = tenancyLicence.getType();
        TenancyLicenceDetails tenancyLicenceDetails = TenancyLicenceDetails.builder()
            .typeOfTenancyLicence(TenancyLicenceType.from(combinedLicenceType))
            .tenancyLicenceDate(tenancyLicence.getStartDate())
            .detailsOfOtherTypeOfTenancyLicence(tenancyLicence.getOtherTypeDetails())
            .build();

        pcsCase.setTenancyLicenceDetails(tenancyLicenceDetails);
    }

    private static void setOccupationLicenceFields(PCSCase pcsCase, TenancyLicenceEntity tenancyLicence) {
        CombinedLicenceType combinedLicenceType = tenancyLicence.getType();

        OccupationLicenceDetailsWales occupationLicence = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.from(combinedLicenceType))
            .licenceStartDate(tenancyLicence.getStartDate())
            .otherLicenceTypeDetails(tenancyLicence.getOtherTypeDetails())
            .build();

        pcsCase.setOccupationLicenceDetailsWales(occupationLicence);
    }

}
