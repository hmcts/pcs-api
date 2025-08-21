package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppEventLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppEventService;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.roles.service.UserInfoService;

@Component
public class UpdateGenApp1 extends AbstractUpdateGenApp {

    public UpdateGenApp1(GenAppService genAppService,
                         UserInfoService userInfoService,
                         GenAppEventLogService genAppEventLogService,
                         PcsCaseService pcsCaseService,
                         GenAppEventService genAppEventService) {

        super(genAppService, userInfoService, genAppEventLogService, pcsCaseService, genAppEventService);
    }

    @Override
    protected int getGenAppIndex() {
        return 1;
    }

}
