package uk.gov.hmcts.reform.pcs.hearings.messaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.hearings.model.HmcHearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

import static org.springframework.util.StringUtils.hasText;

/**
 * Verifies the HMC notification path by retrieving the hearing from HMC. It does not update PCS hearing state, so it
 * must only be enabled where notifications can be discarded. It refuses to start without a deployment id or on the
 * shared PCS subscription.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = {"hmc.service-bus.enabled", "hmc.service-bus.retrieve-only"}, havingValue = "true")
public class HmcHearingRetrievalHandler implements HmcHearingNotificationHandler {

    static final String SHARED_SUBSCRIPTION_PREFIX = "hmc-to-pcs-subscription-";

    private final HmcHearingService hmcHearingService;
    private final String deploymentId;
    private final String subscription;

    public HmcHearingRetrievalHandler(HmcHearingService hmcHearingService,
                                      @Value("${hmc.deployment-id}") String deploymentId,
                                      @Value("${hmc.service-bus.subscription}") String subscription) {
        this.hmcHearingService = hmcHearingService;
        this.deploymentId = deploymentId;
        this.subscription = subscription;
    }

    @PostConstruct
    void validateActivation() {
        if (!hasText(deploymentId)) {
            throw new IllegalStateException("Retrieve-only HMC notification handling requires hmc.deployment-id");
        }
        if (subscription != null && subscription.startsWith(SHARED_SUBSCRIPTION_PREFIX)) {
            throw new IllegalStateException(
                "Retrieve-only HMC notification handling must not consume the shared subscription " + subscription);
        }
        log.warn("HMC hearing notifications are only retrieved from HMC and PCS hearing state is NOT updated");
    }

    @Override
    public void handle(HmcHearingResponse notification) {
        hmcHearingService.getHearing(notification.getHearingID());
        log.info("Retrieved hearing {} from HMC after notification", notification.getHearingID());
    }
}
