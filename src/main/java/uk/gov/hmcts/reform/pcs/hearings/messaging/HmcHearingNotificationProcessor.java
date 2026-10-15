package uk.gov.hmcts.reform.pcs.hearings.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

@Slf4j
@Service
@ConditionalOnProperty(name = "hmc.service-bus.enabled", havingValue = "true")
public class HmcHearingNotificationProcessor {

    private final HmcHearingService hmcHearingService;

    public HmcHearingNotificationProcessor(HmcHearingService hmcHearingService) {
        this.hmcHearingService = hmcHearingService;
    }

    public void process(HmcHearingResponse notification) {
        hmcHearingService.getHearing(notification.getHearingID());
        log.info("Retrieved hearing {} from HMC after notification", notification.getHearingID());
    }
}
