package uk.gov.hmcts.reform.pcs.hearings.messaging;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

@Slf4j
@Component
@ConditionalOnProperty(name = "hmc.service-bus.enabled", havingValue = "true")
public class HmcServiceBusActivation {

    private final ServiceBusProcessorClient processorClient;
    private final FeatureToggleService featureToggleService;

    public HmcServiceBusActivation(ServiceBusProcessorClient processorClient,
                                   FeatureToggleService featureToggleService) {
        this.processorClient = processorClient;
        this.featureToggleService = featureToggleService;
    }

    @Scheduled(fixedDelayString = "${hmc.service-bus.activation-check-interval:PT30S}")
    public void reconcile() {
        boolean enabled = featureToggleService.isEnabled(FeatureFlag.HMC_INTEGRATION);
        boolean running = processorClient.isRunning();

        if (enabled && !running) {
            processorClient.start();
            log.info("HMC integration enabled: started consuming {}/{}", processorClient.getTopicName(),
                     processorClient.getSubscriptionName());
        } else if (!enabled && running) {
            processorClient.stop();
            log.info("HMC integration disabled: stopped consuming {}/{}", processorClient.getTopicName(),
                     processorClient.getSubscriptionName());
        }
    }
}
