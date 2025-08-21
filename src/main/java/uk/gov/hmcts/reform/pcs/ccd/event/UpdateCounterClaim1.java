package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimEventLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.CounterClaimEventService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.roles.service.UserInfoService;

@Component
public class UpdateCounterClaim1 extends AbstractUpdateCounterClaim {

    public UpdateCounterClaim1(ClaimService claimService,
                               UserInfoService userInfoService,
                               ClaimEventLogService claimEventLogService,
                               PcsCaseService pcsCaseService,
                               CounterClaimEventService counterClaimEventService) {

        super(claimService, userInfoService, claimEventLogService, pcsCaseService, counterClaimEventService);
    }

    @Override
    protected int getClaimIndex() {
        return 1;
    }

}
