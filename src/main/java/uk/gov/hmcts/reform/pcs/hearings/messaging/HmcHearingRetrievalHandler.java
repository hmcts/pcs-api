package uk.gov.hmcts.reform.pcs.hearings.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

@Slf4j
@Component
@ConditionalOnProperty(name = "hmc.service-bus.enabled", havingValue = "true")
public class HmcHearingRetrievalHandler implements HmcHearingNotificationHandler {

    private final HmcHearingService hmcHearingService;

    public HmcHearingRetrievalHandler(HmcHearingService hmcHearingService) {
        this.hmcHearingService = hmcHearingService;
    }

    @Override
    public void handle(HmcHearingResponse notification) {
        hmcHearingService.getHearing(notification.getHearingID());
        log.info("Retrieved hearing {} for case {} from HMC", notification.getHearingID(),
                 notification.getCaseRef());
    }
}
