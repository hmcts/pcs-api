package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotApplicable;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.HousingActWalesEntity;

@Service
public class HousingActWalesService {

    public HousingActWalesEntity createHousingActWalesEntity(PCSCase pcsCase) {
        WalesHousingAct walesHousingAct = pcsCase.getWalesHousingAct();

        if (walesHousingAct == null || walesHousingAct.getRegistered() == null) {
            return null;
        }

        HousingActWalesEntity housingActWalesEntity = new HousingActWalesEntity();

        housingActWalesEntity.setRegistered(walesHousingAct.getRegistered());
        if (walesHousingAct.getRegistered() == YesNoNotApplicable.YES) {
            housingActWalesEntity.setRegistrationNumber(walesHousingAct.getRegistrationNumber());
        }

        housingActWalesEntity.setLicensed(walesHousingAct.getLicensed());
        if (walesHousingAct.getLicensed() == YesNoNotApplicable.YES) {
            housingActWalesEntity.setLicenceNumber(walesHousingAct.getLicenceNumber());
        }

        housingActWalesEntity.setAgentAppointed(walesHousingAct.getLicensedAgentAppointed());
        if (walesHousingAct.getLicensedAgentAppointed() == YesNoNotApplicable.YES) {
            housingActWalesEntity.setAgentFirstName(walesHousingAct.getAgentFirstName());
            housingActWalesEntity.setAgentLastName(walesHousingAct.getAgentLastName());
            housingActWalesEntity.setAgentLicenceNumber(walesHousingAct.getAgentLicenceNumber());
            housingActWalesEntity.setAgentAppointmentDate(walesHousingAct.getAgentAppointmentDate());
        }

        return housingActWalesEntity;
    }

}
