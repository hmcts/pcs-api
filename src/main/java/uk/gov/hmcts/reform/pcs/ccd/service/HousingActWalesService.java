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

        if (walesHousingAct == null || walesHousingAct.getIsExemptLandlord() == null) {
            return null;
        }

        HousingActWalesEntity housingActWalesEntity = new HousingActWalesEntity();
        housingActWalesEntity.setIsExemptLandlord(walesHousingAct.getIsExemptLandlord());

        return housingActWalesEntity;
    }

}
