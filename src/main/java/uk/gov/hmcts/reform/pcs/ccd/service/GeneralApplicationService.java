package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.utils.YesOrNoToBoolean;

@Service
public class GeneralApplicationService {

    public GeneralApplication buildTGeneralApplication(PCSCase pcsCase) {
        return GeneralApplication.builder()
                .generalApplicationWanted(YesOrNoToBoolean.convert(pcsCase.getGeneralApplicationWanted()))
                .build();
    }

}
