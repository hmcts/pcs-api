package uk.gov.hmcts.reform.pcs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;

@Configuration
@ConditionalOnProperty(name = "spring.jms.servicebus.enabled", havingValue = "true", matchIfMissing = false)
public class HearingsJMSConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingsJMSConfiguration.class);

    @JmsListener(destination = "${spring.jms.servicebus.hearings-topic}",
        containerFactory = "topicJmsListenerContainerFactory",
        subscription = "${spring.jms.servicebus.hearings-subscription}")
    public void receiveMessage(String message) {

        LOGGER.info("Message received: {}", message);
    }

}
